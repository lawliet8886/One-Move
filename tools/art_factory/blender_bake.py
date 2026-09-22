"""Offline GLB/FBX -> fixed-pivot RGBA frames. Run Blender with --disable-autoexec.
Uses the existing One Move lighting studio; never calls Tripo or edits app assets.
"""
from __future__ import annotations
import argparse, hashlib, importlib.util, json, math, os, sys
from pathlib import Path
import bpy
from mathutils import Vector


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_studio(output: Path):
    os.environ['ONE_MOVE_ART_OUT'] = str(output / 'studio')
    script = Path(__file__).resolve().parents[1] / 'art' / 'render_mascots.py'
    spec = importlib.util.spec_from_file_location('onemove_original_studio', script)
    studio = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(studio)
    return studio


def import_source(path: Path):
    if not path.is_file() or path.stat().st_size > 100 * 1024 * 1024:
        raise ValueError('A local source file of at most 100 MiB is required')
    before = set(bpy.data.objects)
    if path.suffix.lower() == '.glb':
        bpy.ops.import_scene.gltf(filepath=str(path))
    elif path.suffix.lower() == '.fbx':
        bpy.ops.import_scene.fbx(filepath=str(path), use_anim=True)
    else:
        raise ValueError('Only local GLB or FBX inputs are supported')
    imported = set(bpy.data.objects) - before
    for obj in list(imported):
        if obj.type in {'CAMERA', 'LIGHT'}:
            bpy.data.objects.remove(obj, do_unlink=True)
    meshes = [o for o in bpy.context.scene.objects if o.type == 'MESH']
    if not meshes:
        raise ValueError('Source contains no renderable mesh')
    return {'objects': [{'name': o.name, 'type': o.type} for o in bpy.context.scene.objects
                        if o.type not in {'LIGHT', 'CAMERA'}],
            'actions': [a.name for a in bpy.data.actions],
            'vertices': sum(len(o.data.vertices) for o in meshes),
            'polygons': sum(len(o.data.polygons) for o in meshes)}


def bind_action(binding):
    obj = bpy.data.objects.get(binding['object'])
    action = bpy.data.actions.get(binding['action'])
    if obj is None or action is None:
        raise ValueError(f'Explicit action binding missing: {binding}')
    obj.animation_data_create()
    for track in obj.animation_data.nla_tracks:
        track.mute = True
    obj.animation_data.action = action
    if hasattr(action, 'slots') and len(action.slots) == 1:
        obj.animation_data.action_slot = action.slots[0]


def validate_recipe(recipe):
    if not 64 <= recipe['cell_size'] <= 512:
        raise ValueError('cell_size must be 64..512')
    if not 1 <= recipe['samples'] <= 96 or not 1 <= recipe['fps'] <= 30:
        raise ValueError('Unsafe samples or FPS')
    if not 0 < recipe['body_radius'] or not 2.1 <= recipe['framing'] <= 6:
        raise ValueError('Explicit body radius and framing are required')
    clips = recipe['clips']
    if not clips or not 1 <= sum(len(c['frames']) for c in clips) <= 64:
        raise ValueError('Use a pilot with 1..64 frames, not an unbounded batch')
    names = [c['name'] for c in clips]
    if len(names) != len(set(names)) or any(not n.replace('_', '').isalnum() for n in names):
        raise ValueError('Unique safe clip names are required')
    for clip in clips:
        if not clip['frames'] or any(not isinstance(f, int) or f < 1 for f in clip['frames']):
            raise ValueError('Positive integer source frames are required')


def check_anchor(recipe):
    anchor = recipe.get('root_anchor')
    if not anchor:
        return
    obj = bpy.data.objects[anchor['object']]
    point = obj.matrix_world @ obj.pose.bones[anchor['bone']].head
    if (point - Vector(recipe['body_center'])).length > 0.01 * recipe['body_radius']:
        raise ValueError('Root motion moves the physical anchor; prepare an in-place clip first')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--recipe', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--inspect-only', action='store_true')
    args = parser.parse_args(sys.argv[sys.argv.index('--') + 1:])
    source, out = args.source.resolve(), args.out.resolve()
    if out.exists():
        raise ValueError('Output exists: use a new version directory; nothing overwritten')
    out.mkdir(parents=True)
    studio = load_studio(out)
    scene = studio.setup_scene()
    recipe = json.loads(args.recipe.read_text(encoding="utf-8-sig")) if args.recipe else None
    if recipe is not None:
        validate_recipe(recipe)
        scene.render.fps = recipe["fps"]  # Import seconds at the intended sampling rate.
    info = import_source(source)
    info.update(source_file=source.name, source_sha256=digest(source), blender=bpy.app.version_string)
    (out / 'inspection.json').write_text(json.dumps(info, indent=2) + '\n', encoding='utf-8')
    if args.inspect_only:
        print(json.dumps(info, indent=2))
        return
    if args.recipe is None:
        raise ValueError('--recipe is required for baking')
    recipe = json.loads(args.recipe.read_text(encoding='utf-8-sig'))
    validate_recipe(recipe)
    for binding in recipe.get('bindings', []):
        bind_action(binding)
    scene.render.resolution_x = scene.render.resolution_y = recipe['cell_size']
    scene.cycles.samples = recipe['samples']
    scene.render.threads = 1
    scene.render.fps = recipe['fps']
    scene.render.film_transparent = True
    scene.camera.data.type = 'ORTHO'
    scene.camera.data.ortho_scale = recipe['framing'] * recipe['body_radius']
    center = Vector(recipe['body_center'])
    yaw, pitch = map(math.radians, (recipe['camera_yaw_degrees'], recipe['camera_pitch_degrees']))
    direction = Vector((math.sin(yaw)*math.cos(pitch), -math.cos(yaw)*math.cos(pitch), math.sin(pitch)))
    scene.camera.location = center + direction * recipe['body_radius'] * 8
    studio.aim(scene.camera, center)
    frames = []
    for clip in recipe['clips']:
        for index, frame in enumerate(clip['frames']):
            scene.frame_set(frame)
            bpy.context.view_layer.update()
            check_anchor(recipe)
            filename = f"{clip['name']}_{index:03d}.png"
            scene.render.filepath = str(out / filename)
            bpy.ops.render.render(write_still=True)
            frames.append({'file': filename, 'clip': clip['name'], 'index': index,
                           'source_frame': frame, 'sha256': digest(out / filename),
                           'duration_ms': round(1000 / recipe['fps'], 4),
                           'pose_matrices': {o.name: {b.name: [round(v,6) for row in b.matrix for v in row] for b in o.pose.bones}
                                             for o in scene.objects if o.type == 'ARMATURE'}})
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(out / 'baked-source.blend'))
    manifest = {'schema_version': 1, 'kind': 'OFFLINE ART BAKE; NOT ANDROID EVIDENCE',
                'source': info, 'recipe': recipe, 'recipe_sha256': digest(args.recipe), 'frames': frames}
    (out / 'bake.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(f'OFFLINE_BAKE_COMPLETE: {len(frames)} frames. No network generation.')

if __name__ == '__main__':
    main()

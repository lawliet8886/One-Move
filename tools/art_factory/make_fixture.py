"""Create a free LOCAL compatibility fixture from the existing original Pip helpers.
The simple in-place bone animation is test scaffolding, not final character acting.
"""
from pathlib import Path
import argparse, json, math, sys
import bpy
sys.path.insert(0, str(Path(__file__).resolve().parent))
from blender_bake import load_studio, digest

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--out', type=Path, required=True)
args = parser.parse_args(sys.argv[sys.argv.index('--')+1:])
out = args.out.resolve()
if out.exists():
    raise ValueError('Use a new fixture output directory')
out.mkdir(parents=True)
studio = load_studio(out)
scene = studio.setup_scene()
body = studio.material('pip soft fabric', (0.81,0.39,0.105), 0.76, True)
cream = studio.material('Cream porcelain', (1,0.895,0.715), 0.5)
pink = studio.material('Rose lining', (0.93,0.46,0.44), 0.75)
ink = studio.material('Dark polished eyes', (0.017,0.028,0.032), 0.24)
studio.sphere('pip physical body', (0,0,0), (1,0.79,1), body)
studio.sphere('Cream face bib', (0,-0.67,-0.20), (0.68,0.245,0.52), cream)
for side in (-1,1):
    studio.sphere('Inset plush foot', (side*0.35,-0.44,-0.70), (0.23,0.25,0.19), body)
    studio.sphere('Little arm', (side*0.83,-0.28,-0.27), (0.15,0.24,0.21), body)
    studio.ear(side,body,pink)
studio.face('neutral',ink,cream,pink)
bpy.ops.object.select_all(action='DESELECT')
for obj in scene.objects:
    obj.select_set(obj.type in {'MESH','CURVE'})
bpy.context.view_layer.objects.active = next(o for o in scene.objects if o.select_get())
bpy.ops.object.convert(target='MESH')
meshes = [o for o in scene.objects if o.type == 'MESH']
bpy.ops.object.select_all(action='DESELECT')
arm = bpy.data.armatures.new('Pip_FixtureRig')
rig = bpy.data.objects.new('Pip_Rig', arm)
bpy.context.collection.objects.link(rig)
rig.select_set(True)
bpy.context.view_layer.objects.active = rig
bpy.ops.object.mode_set(mode='EDIT')
bone = arm.edit_bones.new('body')
bone.head, bone.tail = (0,0,0), (0,0,0.7)
bpy.ops.object.mode_set(mode='OBJECT')
for obj in meshes:
    obj.parent = rig
    group = obj.vertex_groups.new(name='body')
    group.add(list(range(len(obj.data.vertices))),1.0,'REPLACE')
    modifier = obj.modifiers.new('Fixture body rig','ARMATURE')
    modifier.object = rig
pose = rig.pose.bones['body']
pose.rotation_mode = 'XYZ'
for frame in range(1,33):
    phase, t = (frame-1)//8, (frame-1)%8 / 8 * math.tau
    pose.rotation_euler = ((0.12 if phase==2 else 0)*math.sin(t), (0.10 if phase==3 else 0.025)*math.sin(t), 0)
    stretch = 1 + (0.09 if phase==1 else 0.01)*math.sin(t)
    pose.scale = (1/math.sqrt(stretch),1/math.sqrt(stretch),stretch)
    pose.keyframe_insert(data_path='rotation_euler', frame=frame)
    pose.keyframe_insert(data_path='scale', frame=frame)
rig.animation_data.action.name = 'OneMove_fixture'
scene.frame_start, scene.frame_end, scene.render.fps = 1,32,12
scene.frame_set(1)
bpy.ops.object.select_all(action='DESELECT')
rig.select_set(True)
for obj in meshes:
    obj.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'pip-fixture.blend'))
bpy.ops.export_scene.gltf(filepath=str(out/'pip-fixture.glb'), export_format='GLB',
                          use_selection=True, export_animations=True)
rig.animation_data.action = bpy.data.actions['OneMove_fixture']
if hasattr(rig.animation_data.action, 'slots'):
    rig.animation_data.action_slot = rig.animation_data.action.slots[0]
bpy.context.view_layer.objects.active = rig
bpy.ops.export_scene.fbx(filepath=str(out/'pip-fixture.fbx'), use_selection=True,
                         bake_anim=True, bake_anim_use_all_actions=True, bake_anim_use_nla_strips=False, add_leaf_bones=False,
                         path_mode='COPY', embed_textures=True)
record = {'kind':'ORIGINAL LOCAL TEST FIXTURE, NOT TRIPO ART OR ANDROID EVIDENCE',
          'credits_spent':0, 'source_helpers':'tools/art/render_mascots.py',
          'files':{p.name:digest(p) for p in out.glob('pip-fixture.*')},
          'animation':'single-body in-place bone test, not final idle/jump/fall/happy performance'}
(out/'fixture.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
print('FREE_LOCAL_FIXTURE_EXPORTED_GLB_FBX')

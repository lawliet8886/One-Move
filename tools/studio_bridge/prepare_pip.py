"""Prepare the downloaded Studio pilot offline, preserving its untouched GLB source."""
import argparse,hashlib,importlib.util,json,math,os,sys
from pathlib import Path
import bpy
from mathutils import Vector
p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--out',type=Path,required=True)
a=p.parse_args(sys.argv[sys.argv.index('--')+1:]);out=a.out.resolve()
if out.exists(): raise ValueError('Choose a new evidence directory')
out.mkdir(parents=True);os.environ['ONE_MOVE_ART_OUT']=str(out/'studio')
script=Path(__file__).resolve().parents[1]/'art/render_mascots.py'
spec=importlib.util.spec_from_file_location('existing_studio',script);studio=importlib.util.module_from_spec(spec);spec.loader.exec_module(studio)
scene=studio.setup_scene();bpy.ops.import_scene.gltf(filepath=str(a.source.resolve()))
meshes=[o for o in scene.objects if o.type=='MESH']
report={'source_sha256':hashlib.sha256(a.source.read_bytes()).hexdigest(),'source_file':a.source.name,
        'origin':'Tripo Studio paid pilot; offline derivatives','credits_spent_offline':0,'meshes':[]}
for obj in meshes:
    before=len(obj.data.polygons);bpy.context.view_layer.objects.active=obj
    modifier=obj.modifiers.new('offline_preview_reduction','DECIMATE');modifier.ratio=min(1,65000/max(before,1))
    bpy.ops.object.modifier_apply(modifier=modifier.name)
    for poly in obj.data.polygons: poly.use_smooth=True
    report['meshes'].append({'name':obj.name,'faces_before':before,'faces_after':len(obj.data.polygons)})
report['bounds']=[{'name':o.name,'min':[min((o.matrix_world@Vector(c))[i] for c in o.bound_box) for i in range(3)],
    'max':[max((o.matrix_world@Vector(c))[i] for c in o.bound_box) for i in range(3)]} for o in meshes]
report['images']=[{'name':i.name,'size':list(i.size)} for i in bpy.data.images if i.type=='IMAGE']
report['actions']=[act.name for act in bpy.data.actions]
scene.render.resolution_x=scene.render.resolution_y=192;scene.cycles.samples=12;scene.render.threads=1
scene.render.film_transparent=True;scene.camera.data.type='ORTHO';scene.camera.data.ortho_scale=0.42*3.6
center=Vector((0,0,0.40));report['renders']=[]
for name,degrees in [('front',0),('three_quarter',30),('back',180)]:
    angle=math.radians(degrees)
    scene.camera.location=center+Vector((math.sin(angle)*3,-math.cos(angle)*3,0.15))
    studio.aim(scene.camera,center);scene.render.filepath=str(out/(name+'.png'))
    bpy.ops.render.render(write_still=True)
    report['renders'].append({'file':name+'.png','sha256':hashlib.sha256((out/(name+'.png')).read_bytes()).hexdigest()})
bpy.ops.file.pack_all();bpy.ops.wm.save_as_mainfile(filepath=str(out/'pip-studio-preview.blend'))
report['approved_for_runtime']=False
report['note']='Inspection candidate only. No facial rig or animations are invented. Simplification must pass silhouette/material review.'
(out/'preview-report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report))

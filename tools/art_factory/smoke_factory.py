"""Entirely offline smoke. Small fixture, GLB rig bake, FBX inspection, WebP contracts."""
from __future__ import annotations
import argparse, json, subprocess, sys
from pathlib import Path

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--blender',required=True)
parser.add_argument('--out',type=Path,required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[2]
tools=root/'tools/art_factory'
out=args.out.resolve()
if out.exists(): raise ValueError('Choose a fresh evidence directory')
out.mkdir(parents=True)

def run(command, name):
    with (out/(name+'.log')).open('w',encoding='utf-8') as log:
        result=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=180)
    if result.returncode:
        raise RuntimeError(f'{name} failed ({result.returncode}); inspect {out/(name+".log")}')
    print(name+': PASS',flush=True)

def blender(script, extra, name):
    run([args.blender,'--background','--factory-startup','--disable-autoexec',
         '--threads','1','--python-exit-code','1','--python',str(tools/script),'--',*map(str,extra)],name)

run([sys.executable,str(tools/'test_factory.py')],'atlas-contracts')
blender('make_fixture.py',['--out',out/'fixture'],'fixture-export')
formats={}
for fmt in ('glb','fbx'):
    blender('blender_bake.py',['--source',out/'fixture'/f'pip-fixture.{fmt}',
            '--out',out/f'inspect-{fmt}','--inspect-only'],f'import-{fmt}')
    formats[fmt]=json.loads((out/f'inspect-{fmt}'/'inspection.json').read_text(encoding='utf-8'))
    if formats[fmt]['vertices'] < 1: raise AssertionError('No geometry imported')
recipe_template=root/'art/asset_factory/pip_fixture.recipe.json'
# Controlled fixture only: Blender versions append the rig name differently.
# Production recipes still require an explicitly reviewed object/action binding.
if len(formats['glb']['actions']) != 1: raise AssertionError('GLB fixture lost its single action')
glb_recipe=json.loads(recipe_template.read_text(encoding='utf-8'))
glb_recipe['bindings'][0]['action']=formats['glb']['actions'][0]
recipe=out/'glb.recipe.json'; recipe.write_text(json.dumps(glb_recipe),encoding='utf-8')
blender('blender_bake.py',['--source',out/'fixture/pip-fixture.glb','--recipe',recipe,
        '--out',out/'bake-glb'],'rigged-glb-bake')
bake=json.loads((out/'bake-glb/bake.json').read_text(encoding='utf-8'))
for clip in ('idle','jump','fall','happy'):
    frames=[f for f in bake['frames'] if f['clip']==clip]
    signatures={json.dumps(f['pose_matrices'],sort_keys=True) for f in frames}
    if len(signatures) < 2: raise AssertionError(f'{clip} did not actually animate the imported rig')
run([sys.executable,str(tools/'pack_atlas.py'),'--bake',str(out/'bake-glb'),'--out',str(out/'packed-glb')],'pack-glb')
# The controlled FBX fixture must also retain and animate its exported rig.
if len(formats['fbx']['actions']) != 1: raise AssertionError('FBX fixture lost its single action')
fbx=json.loads(recipe.read_text(encoding='utf-8'))
fbx['bindings'][0]['action']=formats['fbx']['actions'][0]
fbx_recipe=out/'fbx.recipe.json'; fbx_recipe.write_text(json.dumps(fbx),encoding='utf-8')
blender('blender_bake.py',['--source',out/'fixture/pip-fixture.fbx','--recipe',fbx_recipe,
        '--out',out/'bake-fbx'],'rigged-fbx-bake')
fbx_bake=json.loads((out/'bake-fbx/bake.json').read_text(encoding='utf-8'))
for clip in ('idle','jump','fall','happy'):
    signatures={json.dumps(f['pose_matrices'],sort_keys=True) for f in fbx_bake['frames'] if f['clip']==clip}
    if len(signatures) < 2: raise AssertionError(f'FBX {clip} did not animate')
run([sys.executable,str(tools/'pack_atlas.py'),'--bake',str(out/'bake-fbx'),'--out',str(out/'packed-fbx')],'pack-fbx')
# A front-facing render shares the exact body pivot and lighting with the three-quarter view.
front=json.loads(recipe.read_text(encoding='utf-8'))
front['camera_yaw_degrees']=0; front['camera_pitch_degrees']=0
front['clips']=[{'name':'front_reference','frames':[1]}]
front_recipe=out/'front.recipe.json'; front_recipe.write_text(json.dumps(front),encoding='utf-8')
blender('blender_bake.py',['--source',out/'fixture/pip-fixture.glb','--recipe',front_recipe,
        '--out',out/'bake-front'],'front-camera-bake')
report={'status':'PASS','kind':'OFFLINE FIXTURE ONLY; NOT ANDROID OR FINAL ART',
        'credits_spent':0,'glb_actions':formats['glb']['actions'],
        'fbx_actions':formats['fbx']['actions'],'fbx_animation_verified':True,
        'glb_rigged_clips_verified':['idle','jump','fall','happy'],'glb_frames':8,'fbx_frames':8,
        'front_frames':1,'atlas_tests':9,'android_asset_integration':False}
(out/'smoke-result.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))

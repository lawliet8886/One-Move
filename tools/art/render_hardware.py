"""Original workshop hardware. Offline Blender renders, never Tripo or Android footage.
Three fixed-pivot transparent cells; source scenes stay outside the app.
"""
from pathlib import Path
import hashlib, importlib.util, json, math, os
import bpy
from mathutils import Vector
OUT = Path(os.environ['ONE_MOVE_HARDWARE_OUT']).resolve()
if OUT.exists(): raise ValueError('Use a new output directory; reviewed assets are never overwritten')
OUT.mkdir(parents=True)
os.environ['ONE_MOVE_ART_OUT'] = str(OUT / 'studio')
spec = importlib.util.spec_from_file_location('original_studio', Path(__file__).with_name('render_mascots.py'))
studio = importlib.util.module_from_spec(spec); spec.loader.exec_module(studio)
SIZE, FRAMING = 256, 2.4

def metal(name, color, roughness=0.34, metallic=0.78):
    mat = studio.material(name, color, roughness)
    mat.node_tree.nodes['Principled BSDF'].inputs['Metallic'].default_value = metallic
    return mat

def cylinder(name, location, radius, depth, mat):
    bpy.ops.mesh.primitive_cylinder_add(vertices=64, radius=radius, depth=depth, location=location, rotation=(math.pi/2,0,0))
    obj=bpy.context.object; obj.name=name; obj.data.materials.append(mat)
    bevel=obj.modifiers.new('machined rounded edges','BEVEL'); bevel.width=0.025; bevel.segments=3
    obj.modifiers.new('weighted normals','WEIGHTED_NORMAL')
    return obj

def ring(name, radius, tube, mat, rotation=(0,0,0), location=(0,0,0)):
    bpy.ops.mesh.primitive_torus_add(major_segments=80,minor_segments=12,major_radius=radius,minor_radius=tube,location=location,rotation=rotation)
    obj=bpy.context.object; obj.name=name; obj.data.materials.append(mat)
    for face in obj.data.polygons: face.use_smooth=True
    return obj

def scene_setup():
    scene=studio.setup_scene()
    scene.cycles.samples=64; scene.render.threads=1
    scene.render.resolution_x=scene.render.resolution_y=SIZE
    scene.camera.data.ortho_scale=FRAMING
    scene.view_settings.exposure=-0.10
    return scene

def wrecker():
    iron=metal('blue black enamel iron',(0.032,0.071,0.086),0.29,0.66)
    brass=metal('satin brass bands',(0.68,0.35,0.095),0.29)
    dark=metal('recessed screw slots',(0.013,0.026,0.028),0.58)
    studio.sphere('calibrated weight sphere',(0,0,0),(0.97,0.97,0.97),iron)
    ring('equatorial brass strap',0.959,0.038,brass)
    ring('vertical brass strap',0.959,0.025,brass,rotation=(0,math.pi/2,0))
    cylinder('front brass boss',(0,-0.97,0),0.23,0.055,brass)
    cylinder('dark centre inset',(0,-1.005,0),0.145,0.024,dark)
    cylinder('square-ended axle cap',(0,-1.025,0),0.077,0.026,brass)
    for x in (-0.62,0.62):
        y=-math.sqrt(0.97**2-x*x)
        cylinder('belt rivet',(x,y-0.015,0),0.066,0.04,brass)
    for z in (-0.49,0.49):
        y=-math.sqrt(0.97**2-z*z)
        cylinder('meridian rivet',(0,y-0.015,z),0.049,0.028,brass)

def stone():
    mat=metal('river stone ceramic',(0.27,0.34,0.30),0.84,0.08)
    nodes,links=mat.node_tree.nodes,mat.node_tree.links
    noise=nodes.new('ShaderNodeTexNoise'); noise.inputs['Scale'].default_value=9
    noise.inputs['Detail'].default_value=3
    bump=nodes.new('ShaderNodeBump'); bump.inputs['Strength'].default_value=0.26
    bump.inputs['Distance'].default_value=0.065
    links.new(noise.outputs['Fac'],bump.inputs['Height'])
    links.new(bump.outputs['Normal'],nodes['Principled BSDF'].inputs['Normal'])
    studio.sphere('round collision-aligned stone',(0,0,0),(0.995,0.995,0.995),mat)
    vein=studio.material('stone pale inclusions',(0.42,0.46,0.32),0.88)
    for x,z,r in [(-0.36,0.20,0.14),(0.30,-0.32,0.18),(0.20,0.46,0.075)]:
        y=-math.sqrt(0.995**2-x*x-z*z)
        studio.sphere('subtle shallow inclusion',(x,y,z),(r,0.018,r*0.65),vein)

def axle():
    brass=metal('machined brass bearing',(0.72,0.40,0.12),0.29)
    enamel=metal('dark green bearing inset',(0.023,0.093,0.076),0.30,0.35)
    cylinder('flanged bearing housing',(0,0,0),0.995,0.20,brass)
    cylinder('enamel housing inset',(0,-0.115,0),0.77,0.075,enamel)
    ring('bearing inner lip',0.59,0.045,brass,rotation=(math.pi/2,0,0),location=(0,-0.19,0))
    cylinder('central brass axle',(0,-0.16,0),0.36,0.16,brass)
    cylinder('axle dark inset',(0,-0.25,0),0.13,0.015,enamel)
    for i in range(4):
        a=math.pi/4+i*math.pi/2
        cylinder('housing fastener',(0.87*math.cos(a),-0.125,0.87*math.sin(a)),0.061,0.033,brass)

records=[]
for name,build in [('wrecker',wrecker),('stone',stone),('axle',axle)]:
    scene=scene_setup(); build()
    filename=name+'.png'; scene.render.filepath=str(OUT/filename)
    bpy.ops.render.render(write_still=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/(name+'-source.blend')))
    records.append({'name':name,'file':filename,'sha256':hashlib.sha256((OUT/filename).read_bytes()).hexdigest()})
manifest={'schema_version':1,'kind':'ORIGINAL OFFLINE ART; NOT ANDROID EVIDENCE',
    'source_script':'tools/art/render_hardware.py','source_commit':os.environ.get('ONE_MOVE_SOURCE_SHA','local-uncommitted'),
    'source_script_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
    'blender':bpy.app.version_string,'credits_spent':0,'cell_size':SIZE,'framing':FRAMING,
    'pivot_normalized':[0.5,0.5],'physical_radius':1.0,'sprites':records}
(OUT/'render-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print('HARDWARE_RENDERED: 3 original offline sprites. No network or paid tasks.')

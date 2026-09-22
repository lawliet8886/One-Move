"""Original One Move toy mascots, rendered offline in Blender.

No model API, purchased material or external asset is used. These renders are
ART ASSETS, not screenshots or evidence of Android gameplay. Camera framing is
fixed: the physical body radius 1.0 maps to image_width / 3.6 pixels.
"""
from __future__ import annotations
import hashlib
import json
import math
import os
from pathlib import Path
import bpy
from mathutils import Vector

OUTPUT = Path(os.environ.get("ONE_MOVE_ART_OUT", "art-output")).resolve()
OUTPUT.mkdir(parents=True, exist_ok=True)
SIZE = 384
FRAMING = 3.6


def material(name, color, roughness=0.68, fabric=False):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1)
    mat.use_nodes = True
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    shader = nodes.get("Principled BSDF")
    shader.inputs["Base Color"].default_value = (*color, 1)
    shader.inputs["Roughness"].default_value = roughness
    if fabric:
        noise = nodes.new("ShaderNodeTexNoise")
        noise.inputs["Scale"].default_value = 115
        noise.inputs["Detail"].default_value = 2
        bump = nodes.new("ShaderNodeBump")
        bump.inputs["Strength"].default_value = 0.13
        bump.inputs["Distance"].default_value = 0.025
        links.new(noise.outputs["Fac"], bump.inputs["Height"])
        links.new(bump.outputs["Normal"], shader.inputs["Normal"])
    return mat


def sphere(name, location, scale, mat):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=40, ring_count=24, location=location)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    obj.data.materials.append(mat)
    for face in obj.data.polygons:
        face.use_smooth = True
    return obj


def line(name, points, thickness, mat):
    data = bpy.data.curves.new(name, "CURVE")
    data.dimensions = "3D"
    data.resolution_u = 12
    data.bevel_depth = thickness
    data.bevel_resolution = 3
    spline = data.splines.new("BEZIER")
    spline.bezier_points.add(len(points) - 1)
    for item, point in zip(spline.bezier_points, points):
        item.co = point
        item.handle_left_type = "AUTO"
        item.handle_right_type = "AUTO"
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    data.materials.append(mat)
    return obj


def ear(side, mat, pink):
    x = side * 0.55
    verts = [(x-0.30,-0.25,0.54), (x+0.30,-0.25,0.54),
             (x+side*0.14,-0.11,1.40), (x,-0.02,0.77),
             (x-0.30,0.18,0.54), (x+0.30,0.18,0.54),
             (x+side*0.14,0.16,1.40)]
    mesh = bpy.data.meshes.new("soft_fox_ear")
    mesh.from_pydata(verts, [], [(0,1,2), (4,6,5), (0,4,5,1), (1,5,6,2), (2,6,4,0)])
    mesh.update()
    obj = bpy.data.objects.new("pip_ear", mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(mat)
    bevel = obj.modifiers.new("Rounded fabric seam", "BEVEL")
    bevel.width, bevel.segments = 0.065, 4
    obj.modifiers.new("Weighted corner normals", "WEIGHTED_NORMAL")
    inner = sphere("ear_lining", (x+side*0.04,-0.265,0.99), (0.105,0.035,0.23), pink)
    inner.rotation_euler.y = side*0.18


def leaf(name, location, angle, mat):
    obj = sphere(name, location, (0.19,0.055,0.40), mat)
    obj.rotation_euler.y = angle
    return obj


def face(expression, ink, cream, pink):
    for side in (-1, 1):
        x = side*0.33
        if expression == "happy":
            line("happy_eye", [(x-0.13,-0.845,0.15),(x,-0.895,0.24),(x+0.13,-0.845,0.15)],0.032,ink)
        elif expression == "dizzy":
            for sign in (-1, 1):
                line("dizzy_eye",[(x-0.105,-0.89,0.20-sign*0.10),(x+0.105,-0.89,0.20+sign*0.10)],0.028,ink)
        else:
            big = 1.16 if expression == "surprised" else 1.0
            sphere("eye_cream", (x,-0.745,0.18),(0.215*big,0.18,0.285*big),cream)
            sphere("eye_ink", (x+0.018,-0.910,0.18),(0.112*big,0.070,0.166*big),ink)
            sphere("eye_glint", (x-0.013,-0.975,0.238),(0.036,0.018,0.043),cream)
        sphere("soft_blush",(side*0.58,-0.680,-0.13),(0.14,0.045,0.075),pink)
    sphere("button_nose",(0,-0.932,-0.105),(0.075,0.055,0.055),ink)
    if expression == "surprised":
        sphere("oh_mouth",(0,-0.928,-0.325),(0.095,0.04,0.15),ink)
        sphere("tiny_tongue",(0,-0.961,-0.405),(0.055,0.018,0.033),pink)
    elif expression == "dizzy":
        line("wavy_mouth",[(-0.15,-0.925,-0.32),(-0.04,-0.936,-0.29),(0.065,-0.936,-0.35),(0.16,-0.925,-0.31)],0.021,ink)
    else:
        line("smile",[(-0.155,-0.916,-0.285),(0,-0.946,-0.365),(0.155,-0.916,-0.285)],0.025,ink)


def aim(obj, target=(0,0,0)):
    obj.rotation_euler = (Vector(target)-obj.location).to_track_quat("-Z","Y").to_euler()


def setup_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 32
    scene.cycles.use_denoising = True
    scene.cycles.max_bounces = 4
    scene.render.threads_mode = "FIXED"
    scene.render.threads = 2
    scene.render.resolution_x = scene.render.resolution_y = SIZE
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.exposure = -0.25
    scene.view_settings.gamma = 1
    world = bpy.data.worlds.new("Warm studio ambient")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (0.79,0.85,0.78,1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.35
    for name, pos, energy, size, color in [
        ("Large soft key",(-3,-4,5),430,4,(1.0,0.91,0.79)),
        ("Cool fill",(4,-3,1.5),200,4,(0.73,0.87,1.0)),
        ("Warm rim",(1,3,4),540,3,(1.0,0.83,0.61))]:
        data=bpy.data.lights.new(name,"AREA");data.energy=energy;data.shape="DISK";data.size=size;data.color=color
        obj=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(obj);obj.location=pos;aim(obj)
    camera_data=bpy.data.cameras.new("Fixed body-centred orthographic camera")
    camera=bpy.data.objects.new("Camera",camera_data);bpy.context.collection.objects.link(camera)
    camera.location=(0,-8,0);aim(camera)
    camera_data.type="ORTHO";camera_data.ortho_scale=FRAMING
    scene.camera=camera
    return scene


def render(name, expression):
    scene=setup_scene()
    shades={"pip":(0.81,0.39,0.105),"mochi":(0.24,0.53,0.31),"blobbo":(0.72,0.29,0.34)}
    body=material(name+" soft fabric",shades[name],0.76,True)
    cream=material("Cream porcelain",(1.0,0.895,0.715),0.50)
    pink=material("Rose lining",(0.93,0.46,0.44),0.75)
    ink=material("Dark polished eyes",(0.017,0.028,0.032),0.24)
    leafmat=material("Leaf felt",(0.22,0.41,0.11),0.80,True)
    sphere(name+" physical body",(0,0,0),(1.0,0.79,1.0),body)
    sphere("Cream face bib",(0,-0.67,-0.20),(0.68,0.245,0.52),cream)
    for side in (-1,1):
        sphere("Inset plush foot",(side*0.35,-0.44,-0.70),(0.23,0.25,0.19),body)
        sphere("Little arm",(side*0.83,-0.28,-0.27),(0.15,0.24,0.21),body)
    if name=="pip":
        for side in (-1,1): ear(side,body,pink)
    elif name=="mochi":
        line("Sprout stalk",[(0,0.02,0.82),(0,-0.02,1.17)],0.045,leafmat)
        leaf("Left sprout",(-0.21,0.02,1.20),-0.65,leafmat)
        leaf("Right sprout",(0.18,-0.02,1.26),0.57,leafmat)
    else:
        for side in (-1,1):
            sphere("Round plush ear",(side*0.72,0.01,0.83),(0.30,0.20,0.31),body)
            sphere("Round ear lining",(side*0.73,-0.175,0.85),(0.17,0.035,0.18),pink)
    face(expression,ink,cream,pink)
    filename=f"hero_{name}_{expression}.png"
    scene.render.filepath=str(OUTPUT/filename)
    bpy.ops.render.render(write_still=True)
    if expression=="neutral":
        bpy.ops.wm.save_as_mainfile(filepath=str(OUTPUT/f"{name}_source.blend"))
    path=OUTPUT/filename
    assert path.is_file() and path.stat().st_size>1024, filename
    return {"file":filename,"sha256":hashlib.sha256(path.read_bytes()).hexdigest(),
            "width":SIZE,"height":SIZE,"pivot":[0.5,0.5],"body_radius_pixels":SIZE/FRAMING}


def main():
    assets=[]
    for name in ("pip","mochi","blobbo"):
        for expression in ("neutral","happy","surprised","dizzy"):
            assets.append(render(name,expression))
    manifest={"kind":"Original offline-rendered game art; NOT Android gameplay evidence",
              "source_commit":os.environ.get("GITHUB_SHA","local"),"blender":bpy.app.version_string,
              "body_radius":1.0,"orthographic_framing":FRAMING,"sprites":assets}
    (OUTPUT/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    print("ART_RENDER_COMPLETE: 12 sprite expressions; 3 editable source scenes. No inference API.")


if __name__=="__main__":
    main()

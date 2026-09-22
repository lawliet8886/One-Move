"""Validate three original hardware renders and pack a lossless alpha atlas."""
import argparse, hashlib, json, math
from pathlib import Path
from PIL import Image, ImageDraw
p=argparse.ArgumentParser(); p.add_argument('--source',type=Path,required=True); p.add_argument('--out',type=Path,required=True); args=p.parse_args()
src=args.source.resolve(); out=args.out.resolve()
if out.exists(): raise ValueError('Refuse to overwrite an existing art candidate')
data=json.loads((src/'render-manifest.json').read_text(encoding='utf-8'))
assert [s['name'] for s in data['sprites']]==['wrecker','stone','axle']
assert data['cell_size']==256 and data['framing']==2.4
atlas=Image.new('RGBA',(768,256))
for index,sprite in enumerate(data['sprites']):
    path=src/sprite['file']
    assert hashlib.sha256(path.read_bytes()).hexdigest()==sprite['sha256']
    image=Image.open(path).convert('RGBA'); assert image.size==(256,256)
    alpha=image.getchannel('A'); bounds=alpha.getbbox()
    assert bounds and min(bounds[:2])>2 and max(bounds[2:])<254
    assert alpha.getextrema()==(0,255)
    solid=alpha.point(lambda a:255 if a>=128 else 0).getbbox()
    expected_diameter=256/2.4*2
    assert abs((solid[2]-solid[0])-expected_diameter)<=4
    assert abs((solid[3]-solid[1])-expected_diameter)<=4
    sprite.update(cell=index,alpha_bounds=list(bounds),physical_radius_pixels=256/2.4)
    atlas.paste(image,(index*256,0))
out.mkdir(parents=True)
atlas.save(out/'atlas.webp',format='WEBP',lossless=True,exact=True,method=6)
with Image.open(out/'atlas.webp') as decoded: assert decoded.convert('RGBA').tobytes()==atlas.tobytes()
data.update(atlas_sha256=hashlib.sha256((out/'atlas.webp').read_bytes()).hexdigest(),atlas_width=768,atlas_height=256,decoded_bytes_rgba=768*256*4)
(out/'manifest.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
preview=Image.new('RGB',(768,360),'#183d35'); preview.paste(atlas,(0,28),atlas)
draw=ImageDraw.Draw(preview); draw.text((10,7),'ORIGINAL OFFLINE HARDWARE / NOT ANDROID',fill='white')
for i in range(3):
    for j,size in enumerate((32,48,64)):
        small=atlas.crop((i*256,0,(i+1)*256,256)).resize((size,size),Image.Resampling.LANCZOS)
        preview.paste(small,(i*256+j*72,285),small)
preview.save(out/'review-sheet.png')
print(json.dumps({'status':'PASS','cells':3,'decoded_bytes_rgba':data['decoded_bytes_rgba'],'atlas_bytes':(out/'atlas.webp').stat().st_size,'credits_spent':0}))

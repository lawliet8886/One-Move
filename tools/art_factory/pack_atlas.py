"""Pack validated transparent frames without trimming/recentring the physical pivot."""
from __future__ import annotations
import argparse, hashlib, json, math
from pathlib import Path
from PIL import Image, ImageDraw, features


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def checked_frame(root, entry, cell):
    path = (root / entry['file']).resolve()
    if not path.is_relative_to(root.resolve()) or not path.is_file():
        raise ValueError('Frame escaped bake directory or is missing')
    if digest(path) != entry['sha256']:
        raise ValueError('Frame hash mismatch: ' + entry['file'])
    with Image.open(path) as source:
        if source.mode != 'RGBA' or source.size != (cell, cell):
            raise ValueError('Expected exact-size RGBA frame: ' + entry['file'])
        image = source.copy()
    alpha = image.getchannel('A')
    bounds = alpha.getbbox()
    if bounds is None or alpha.getextrema()[0] != 0:
        raise ValueError('Frame is empty or lacks transparency')
    if bounds[0] < 2 or bounds[1] < 2 or bounds[2] > cell - 2 or bounds[3] > cell - 2:
        raise ValueError('Clipped silhouette; enlarge one shared framing, never crop each frame')
    return image, bounds


def pack(root: Path, out: Path):
    data = json.loads((root / 'bake.json').read_text(encoding='utf-8'))
    recipe, entries = data['recipe'], data['frames']
    cell = recipe['cell_size']
    if not entries or len(entries) > 64 or not 64 <= cell <= 512:
        raise ValueError('Pilot size budget exceeded')
    keys = [(e['clip'], e['index']) for e in entries]
    if len(keys) != len(set(keys)):
        raise ValueError('Duplicate clip frame')
    columns = min(8, len(entries))
    rows = math.ceil(len(entries) / columns)
    dimensions = (cell * columns, cell * rows)
    decoded = dimensions[0] * dimensions[1] * 4
    if decoded > 8 * 1024 * 1024:
        raise ValueError('Decoded atlas exceeds 8 MiB pilot budget')
    if out.exists():
        raise ValueError('Output exists; do not overwrite a reviewed candidate')
    checked = [checked_frame(root, entry, cell) for entry in entries]
    if not features.check('webp'):
        raise ValueError('Pillow WebP support is required')
    atlas = Image.new('RGBA', dimensions)
    clips = {}
    for i, (entry, (image, bounds)) in enumerate(zip(entries, checked)):
        x, y = (i % columns) * cell, (i // columns) * cell
        atlas.paste(image, (x, y))
        clips.setdefault(entry['clip'], []).append({'index': entry['index'], 'rect': [x,y,cell,cell],
            'duration_ms': entry['duration_ms'], 'alpha_bounds': list(bounds), 'pivot_px': [cell/2,cell/2]})
    out.mkdir(parents=True)
    atlas.save(out / 'atlas.webp', format='WEBP', lossless=True, exact=True, method=6)
    atlas.save(out / 'atlas.png')
    with Image.open(out / 'atlas.webp') as decoded_image:
        if decoded_image.convert('RGBA').tobytes() != atlas.tobytes():
            raise ValueError('WebP round-trip changed pixels')
    manifest = {'schema_version': 1, 'asset_id': recipe['asset_id'], 'version': recipe['version'],
        'kind': 'OFFLINE ART CANDIDATE; NOT ANDROID GAMEPLAY', 'approved_for_runtime': False,
        'source': data['source'], 'provenance': recipe['provenance'], 'recipe_sha256': data['recipe_sha256'],
        'atlas_file': 'atlas.webp', 'atlas_sha256': digest(out / 'atlas.webp'),
        'atlas_width': dimensions[0], 'atlas_height': dimensions[1], 'decoded_bytes_rgba': decoded,
        'cell_size': cell, 'pivot_normalized': [0.5,0.5], 'framing': recipe['framing'],
        'body_radius_pixels': cell / recipe['framing'], 'clips': clips,
        'runtime_mapping': 'side_px = physical_radius_px * framing; center on physical body; never translate physics'}
    (out / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    # Contact sheet is explicitly labelled as offline art, never an Android screenshot.
    preview = Image.new('RGB', (max(dimensions[0],512), dimensions[1]+44), '#18352f')
    preview.paste(atlas, (0,44), atlas)
    ImageDraw.Draw(preview).text((10,12), 'OFFLINE ART FIXTURE / NOT ANDROID', fill='white')
    preview.save(out / 'contact-sheet.png')
    return manifest

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bake', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    result = pack(args.bake.resolve(), args.out.resolve())
    print(json.dumps({'asset_id':result['asset_id'], 'decoded_bytes':result['decoded_bytes_rgba'], 'approved':False}))

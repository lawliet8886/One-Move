"""Offline validation tests. No Tripo calls, models or Android claims."""
import json, tempfile, unittest
from pathlib import Path
from PIL import Image, ImageDraw
from pack_atlas import pack, digest

class AtlasContracts(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.bake = self.root/'bake'; self.bake.mkdir()
        self.out = self.root/'packed'
        image = Image.new('RGBA',(64,64))
        ImageDraw.Draw(image).ellipse((14,12,49,52),fill=(220,140,60,255))
        image.save(self.bake/'idle_000.png')
        self.data = {'source':{'source_sha256':'test-only'},'recipe_sha256':'test-only',
            'recipe':{'asset_id':'fixture','version':'test','cell_size':64,'framing':3.6,
                      'provenance':{'credits_spent':0,'origin':'synthetic unit fixture'}},
            'frames':[{'file':'idle_000.png','sha256':digest(self.bake/'idle_000.png'),
                       'clip':'idle','index':0,'duration_ms':100}]}
        self.save()
    def tearDown(self):
        self.temp.cleanup()
    def save(self):
        (self.bake/'bake.json').write_text(json.dumps(self.data),encoding='utf-8')
    def reject(self):
        self.save()
        with self.assertRaises(ValueError):
            pack(self.bake,self.out)
    def test_lossless_alpha_pivot_hash_and_unapproved_gate(self):
        result=pack(self.bake,self.out)
        self.assertFalse(result['approved_for_runtime'])
        self.assertEqual([0.5,0.5],result['pivot_normalized'])
        self.assertEqual([32,32],result['clips']['idle'][0]['pivot_px'])
        self.assertEqual(64*64*4,result['decoded_bytes_rgba'])
        self.assertEqual(digest(self.out/'atlas.webp'),result['atlas_sha256'])
    def test_hash_mismatch_is_rejected(self):
        self.data['frames'][0]['sha256']='wrong'; self.reject()
    def test_duplicate_frames_are_rejected(self):
        self.data['frames']*=2; self.reject()
    def test_traversal_is_rejected(self):
        self.data['frames'][0]['file']='../outside.png'; self.reject()
    def test_empty_atlas_is_rejected(self):
        self.data['frames']=[]; self.reject()
    def test_decoded_memory_budget_is_enforced(self):
        self.data['recipe']['cell_size']=512
        self.data['frames']=[dict(self.data['frames'][0], index=i) for i in range(64)]
        self.reject()
    def test_clipped_silhouette_is_rejected(self):
        image=Image.new('RGBA',(64,64)); ImageDraw.Draw(image).ellipse((0,0,40,40),fill='red')
        image.save(self.bake/'idle_000.png')
        self.data['frames'][0]['sha256']=digest(self.bake/'idle_000.png'); self.reject()
    def test_opaque_image_is_rejected(self):
        Image.new('RGB',(64,64),'red').save(self.bake/'idle_000.png')
        self.data['frames'][0]['sha256']=digest(self.bake/'idle_000.png'); self.reject()
    def test_reviewed_output_is_never_overwritten(self):
        pack(self.bake,self.out); self.reject()

if __name__ == '__main__': unittest.main(verbosity=2)

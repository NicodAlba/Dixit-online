"""Publication inventory: same game IDs/paths, original distinct art, exact licence hashes."""
from pathlib import Path
import hashlib,json,unittest
ROOT=Path(__file__).resolve().parents[2]
class PublicAssetsTest(unittest.TestCase):
    def test_all_visual_outputs_have_matching_provenance_and_card_ids_stay_distinct(self):
        records=json.loads((ROOT/'assets/original-art/outputs.json').read_text())
        recorded={row['path'] for row in records}
        actual={str(p.relative_to(ROOT)) for root in [ROOT/'frontend/public',ROOT/'frontend/src/static/images'] for p in root.rglob('*') if p.suffix.lower() in ('.png','.webp','.jpg','.jpeg','.ico','.svg')}
        self.assertEqual(actual,recorded)
        for row in records:
            self.assertEqual('CC0-1.0',row['license'])
            self.assertEqual(row['sha256'],hashlib.sha256((ROOT/row['path']).read_bytes()).hexdigest())
        cards=[ROOT/f'frontend/public/Cards/{i}.png' for i in range(36)]
        self.assertEqual(36,len({p.read_bytes() for p in cards}))
        for i,p in enumerate(cards):
            self.assertTrue(p.read_bytes().startswith(b'\x89PNG'))
            self.assertIn('<title>',(ROOT/f'assets/original-art/cards/{i}.svg').read_text())
        self.assertFalse((ROOT/'frontend/src/static/fonts/American Typewriter Bold.ttf').exists())
        self.assertIn('SIL OPEN FONT LICENSE',(ROOT/'docs/licenses/BreeSerif-OFL.txt').read_text())

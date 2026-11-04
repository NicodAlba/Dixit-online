from pathlib import Path
import json,tempfile,unittest,sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'operations'))
from retire_acceptance_admins import authorized_candidates,verified_backup
class RetirementGuards(unittest.TestCase):
    def test_only_six_exact_private_synthetic_admin_identities_are_accepted(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder)/'inventory.json';p.chmod(0o600) if p.exists() else None
            rows=[{'id':n+1,'username':'security_'+format(n,'032x'),'authority':'ADMIN'} for n in range(6)]
            def write(value):p.write_text(json.dumps({'candidate_accounts':value}));p.chmod(0o600)
            write(rows);self.assertEqual(6,len(authorized_candidates(p)))
            for altered in (rows[:5],rows+[rows[0]],[dict(row,username='owner') if i==0 else row for i,row in enumerate(rows)]):
                write(altered)
                with self.assertRaises(SystemExit):authorized_candidates(p)
            write(rows);p.chmod(0o644)
            with self.assertRaises(SystemExit):authorized_candidates(p)
    def test_unverified_backup_cannot_authorize_change(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder)/'copy.gpg';p.write_bytes(b'test');p.chmod(0o600)
            p.with_suffix('.gpg.json').write_text(json.dumps({'restored_and_app_verified':False}))
            with self.assertRaises(SystemExit):verified_backup(p)

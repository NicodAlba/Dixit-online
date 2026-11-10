from pathlib import Path
import json
import sys
import tempfile
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "operations"))
from retire_test_accounts import candidates, cleanup_sql

class CleanupGuards(unittest.TestCase):
    def test_only_exact_private_unique_synthetic_player_identities_are_accepted(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "inventory.json"
            row = {"id": 3, "username": "security_" + "a" * 32, "authority": "PLAYER"}
            def write(rows):
                path.write_text(json.dumps({"candidate_accounts": rows})); path.chmod(0o600)
            write([row]); self.assertEqual([row], candidates(path))
            for bad in ([], [row, row], [dict(row, authority="ADMIN")], [dict(row, username="owner")],
                        [dict(row, id=True)], [dict(row, username="security_';DELETE")]):
                write(bad)
                with self.assertRaises(SystemExit): candidates(path)
            write([row]); path.chmod(0o644)
            with self.assertRaises(SystemExit): candidates(path)

    def test_demo_account_requires_exact_private_public_acceptance_evidence(self):
        with tempfile.TemporaryDirectory() as folder:
            proof = Path(folder) / "fixture.json"
            row = {"id": 7, "username": "demo_" + "a" * 20 + "_0", "authority": "PLAYER", "evidence_file": str(proof)}
            inventory = Path(folder) / "inventory.json"
            inventory.write_text(json.dumps({"candidate_accounts": [row]})); inventory.chmod(0o600)
            with self.assertRaises(SystemExit): candidates(inventory)
            proof.write_text(json.dumps({"accounts": [{"id": 7, "username": row["username"], "roles": ["PLAYER"]}]})); proof.chmod(0o600)
            self.assertEqual([row], candidates(inventory))
            proof.write_text(json.dumps({"accounts": [{"id": 8, "username": row["username"], "roles": ["PLAYER"]}]}))
            with self.assertRaises(SystemExit): candidates(inventory)

    def test_rehearsal_is_default_and_does_not_reset_sequences_or_cascade(self):
        rows = [{"id": 3, "username": "security_" + "a" * 32, "authority": "PLAYER"}]
        sql = cleanup_sql(rows)
        self.assertTrue(sql.endswith("ROLLBACK;"))
        self.assertTrue(cleanup_sql(rows, True).endswith("COMMIT;"))
        self.assertNotIn("CASCADE", sql)
        self.assertNotIn("setval", sql)
        self.assertIn("Mixed game with an ordinary account", sql)
        self.assertIn("Exact identity/authority inventory changed", sql)

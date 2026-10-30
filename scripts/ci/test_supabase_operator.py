"""Safety checks only; no external network, credentials or managed database."""
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'operations'))
from supabase_check import Operator, private_config

class ManagedOperatorGuardTest(unittest.TestCase):
    def test_ci_refuses_before_reading_a_config_or_connecting(self):
        for flag in ('CI', 'GITHUB_ACTIONS'):
            with patch.dict(os.environ, {flag: 'true'}, clear=True):
                with self.assertRaises(SystemExit):
                    Operator(Path('/missing-private-config'), Path('/unused'), 'a' * 20)

    def test_config_requires_private_permissions_and_does_not_execute_values(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / '.env.local'
            path.write_text('DIXIT_DB_PASSWORD=$(do-not-execute)\n')
            path.chmod(0o644)
            with self.assertRaises(SystemExit): private_config(path)
            path.chmod(0o600)
            self.assertEqual('$(do-not-execute)', private_config(path)['DIXIT_DB_PASSWORD'])
            path.write_text('PATH=/untrusted\n')
            with self.assertRaises(SystemExit): private_config(path)

    def test_endpoint_role_and_tls_guards_without_network(self):
        ref = 'a' * 20
        values = {'DIXIT_DB_URL': 'jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:5432/postgres',
                  'DIXIT_DB_USERNAME': 'dixit_runtime.' + ref, 'DIXIT_DB_ROLE': 'dixit_runtime',
                  'DIXIT_MIGRATION_DB_USERNAME': 'dixit_migrator.' + ref, 'DIXIT_DB_SSL_MODE': 'verify-full'}
        with tempfile.TemporaryDirectory() as folder, patch.dict(os.environ, {}, clear=True):
            path = Path(folder) / '.env.local'
            def write(config):
                path.write_text(''.join(key+'='+value+'\n' for key,value in config.items()))
                path.chmod(0o600)
            write(values)
            Operator(path, Path('/unused'), ref)
            for changed in ({'DIXIT_DB_URL': values['DIXIT_DB_URL'].replace(':5432', ':6543')},
                            {'DIXIT_DB_USERNAME': 'postgres.' + ref},
                            {'DIXIT_DB_SSL_MODE': 'disable'},
                            {'DIXIT_MIGRATION_DB_SSL_MODE': 'disable'},
                            {'DIXIT_MIGRATION_DB_URL': 'jdbc:postgresql://db.other.supabase.co:5432/postgres'}):
                write(dict(values, **changed))
                with self.assertRaises(SystemExit): Operator(path, Path('/unused'), ref)

if __name__ == '__main__': unittest.main()

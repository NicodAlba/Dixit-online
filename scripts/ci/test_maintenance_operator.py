"""Operator URL guards; never connect or supply real credentials."""
from pathlib import Path
import sys
import unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'operations'))
from maintenance import origin

class MaintenanceOriginTest(unittest.TestCase):
    def test_secure_origins_and_local_operations(self):
        for value in ('https://example.com', 'http://localhost:18080', 'http://127.0.0.1:18080', 'http://[::1]:18080'):
            self.assertEqual(value, origin(value + '/'))

    def test_remote_plaintext_and_embedded_credentials_are_rejected(self):
        for value in ('http://example.com', 'https://user:password@example.com', 'https://example.com/path',
                      'https://example.com?token=private', 'https://example.com#private', '/missing-host'):
            with self.assertRaises(ValueError): origin(value)

if __name__ == '__main__': unittest.main()

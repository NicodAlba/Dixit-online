"""Owner terminal only: verify encrypted backup before hidden Java account recovery."""
import argparse
import os
from pathlib import Path
import subprocess
from supabase_check import private_config
from retire_acceptance_admins import verified_backup

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--config', type=Path, required=True)
parser.add_argument('--jar', type=Path, required=True)
parser.add_argument('--verified-backup', type=Path, required=True)
args = parser.parse_args()
if os.environ.get('GITHUB_ACTIONS') or os.environ.get('CI', '').lower() == 'true':
    raise SystemExit('Owner account recovery is not an ordinary CI operation.')
verified_backup(args.verified_backup)
values = private_config(args.config)
raise SystemExit(subprocess.run(['java', '-Dloader.main=es.us.dp1.lx_xy_24_25.dixit.configuration.ResetAdminPassword',
    '-cp', str(args.jar.resolve()), 'org.springframework.boot.loader.launch.PropertiesLauncher'],
    env=dict(os.environ, **values)).returncode)

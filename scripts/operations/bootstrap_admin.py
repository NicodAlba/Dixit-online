"""Run the packaged one-shot admin bootstrap from an owner's interactive terminal."""
import argparse
import os
from pathlib import Path
import subprocess
from supabase_check import private_config

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--config', type=Path, required=True)
parser.add_argument('--jar', type=Path, required=True)
args = parser.parse_args()
if os.environ.get('GITHUB_ACTIONS') or os.environ.get('CI', '').lower() == 'true':
    raise SystemExit('Owner admin bootstrap is not an ordinary CI operation.')
values = private_config(args.config)
# Inherit the terminal. Java requires Console and hidden, confirmed input.
result = subprocess.run(['java', '-Dloader.main=es.us.dp1.lx_xy_24_25.dixit.configuration.BootstrapAdmin',
                         '-cp', str(args.jar.resolve()), 'org.springframework.boot.loader.launch.PropertiesLauncher'],
                        env=dict(os.environ, **values))
raise SystemExit(result.returncode)

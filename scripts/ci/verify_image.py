"""Run the locally built image only against the disposable CI PostgreSQL service."""
from contextlib import contextmanager
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from zipfile import ZipFile
import json
import os
import re
import secrets
import subprocess
import sys
import time
from history_check import assert_history_unchanged

image, database_container = sys.argv[1:]
root = Path(__file__).resolve().parents[2]
assert os.environ['DIXIT_TEST_POSTGRES_URL'] == 'jdbc:postgresql://127.0.0.1:5432/dixit_ci'
assert os.environ['DIXIT_DB_URL'] == os.environ['DIXIT_TEST_POSTGRES_URL']
assert os.environ['DIXIT_DB_SSL_MODE'] == 'disable'  # CI loopback only, never hosted Supabase.
base = 'http://127.0.0.1:18080'
env = dict(os.environ, PORT='18080', DIXIT_MAINTENANCE_ENABLED='true')
keys = ('DIXIT_DB_URL', 'DIXIT_DB_USERNAME', 'DIXIT_DB_PASSWORD',
        'DIXIT_MIGRATION_DB_USERNAME', 'DIXIT_MIGRATION_DB_PASSWORD',
        'DIXIT_JWT_SECRET', 'DIXIT_DB_SSL_MODE', 'DIXIT_ALLOWED_ORIGINS', 'PORT', 'DIXIT_MAINTENANCE_ENABLED')
secret_keys = ('DIXIT_DB_PASSWORD', 'DIXIT_MIGRATION_DB_PASSWORD', 'DIXIT_JWT_SECRET')

def docker(*args, **kwargs):
    return subprocess.check_output(['docker', *args], text=True, **kwargs).strip()

config = json.loads(docker('image', 'inspect', image, '--format', '{{json .Config}}'))
assert config['User'] == '10001:10001'
assert config['Entrypoint'][0] == 'java' and config['Entrypoint'][-1] == '/app/app.jar'
assert not any(value.startswith(('DIXIT_', 'SPRING_', 'REACT_APP_')) for value in config['Env'])
history = docker('history', '--no-trunc', image)
assert all(os.environ[key] not in history for key in secret_keys)
assert docker('run', '--rm', '--entrypoint', 'id', image, '-u') == '10001'
subprocess.run(['docker', 'run', '--rm', '--entrypoint', 'sh', image, '-c',
                '! command -v mvn && ! command -v npm && ! command -v javac'], check=True)

# Extract the actual final-stage JAR, not the separately packaged host artifact.
container = docker('create', image)
artifact = root / 'target/image/app.jar'
artifact.parent.mkdir(exist_ok=True)
try:
    subprocess.run(['docker', 'cp', container + ':/app/app.jar', str(artifact)], check=True)
finally:
    docker('rm', container)
with ZipFile(artifact) as archive:
    assert b'spring.profiles.default=prod' in archive.read('BOOT-INF/classes/application.properties')
    assert 'BOOT-INF/classes/es/us/dp1/lx_xy_24_25/dixit/configuration/BootstrapAdmin.class' in archive.namelist()
    assert 'BOOT-INF/classes/data.sql' not in archive.namelist()
    assert 'BOOT-INF/classes/application-mysql.properties' not in archive.namelist()
    assert 'BOOT-INF/lib/postgresql-42.7.14.jar' in archive.namelist()
    files = [file for file in (root / 'frontend/build').rglob('*') if file.is_file()]
    assert files
    for file in files:
        assert archive.read('BOOT-INF/classes/static/' + file.relative_to(root / 'frontend/build').as_posix()) == file.read_bytes(), 'Container React resources differ'
print(f'Image runs as UID 10001 with JRE only; production JAR matches all {len(files)} React resources', flush=True)

# No credentials at all must fail immediately, without H2 or ephemeral JWT fallback.
result = subprocess.run(['docker', 'run', '--rm', '--network', 'none', image],
                        capture_output=True, text=True, timeout=45)
(root / 'target/docker-missing-config.log').write_text(result.stdout + result.stderr)
assert result.returncode != 0
assert 'Missing required production configuration:' in result.stdout
assert 'Using an ephemeral' not in result.stdout
print('Image rejects missing production configuration with network disabled', flush=True)

@contextmanager
def server(label):
    name = 'dixit-image-' + secrets.token_hex(6)
    log_path = root / f'target/docker-{label}.log'
    command = ['create', '--name', name, '--network', 'host', '--read-only',
               '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m', '--cap-drop', 'ALL',
               '--security-opt', 'no-new-privileges', '--memory', '512m', '--pids-limit', '256']
    for key in keys:
        command += ['--env', key]  # Values inherited privately, never command arguments.
    docker(*command, image, '--server.address=127.0.0.1', env=env)
    with log_path.open('w') as log:
        process = subprocess.Popen(['docker', 'start', '--attach', name], stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 120
            while True:
                assert process.poll() is None, 'Image exited; inspect ' + log_path.name
                try:
                    with urlopen(base + '/actuator/health', timeout=2) as response:
                        assert json.load(response) == {'status': 'UP'}
                    break
                except (URLError, TimeoutError):
                    assert time.monotonic() < deadline, 'Image health timeout; inspect ' + log_path.name
                    time.sleep(.5)
            assert 'Tomcat started on port 18080' in log_path.read_text()
            yield
        finally:
            docker('stop', '--time', '25', name)
            process.wait(timeout=30)
            state = json.loads(docker('inspect', name, '--format', '{{json .State}}'))
            docker('rm', name)
            assert not state['OOMKilled'], 'Image exceeded its 512 MB test limit'
            assert state['ExitCode'] in (0, 143), 'Image failed graceful termination'
    assert all(os.environ[key] not in log_path.read_text() for key in secret_keys)

def request(path, token=None, body=None, method='GET', expected=200):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = Request(base + path, headers=headers, method=method,
                  data=None if body is None else json.dumps(body).encode())
    try:
        with urlopen(req, timeout=10) as response:
            assert response.status == expected
            content = response.read()
            return json.loads(content) if response.headers.get_content_type() == 'application/json' else content
    except HTTPError as error:
        assert error.code == expected, (path, error.code, expected)
        return None

# Verify the packaged one-shot CLI fails safely without an interactive terminal.
bootstrap = subprocess.run(['docker', 'run', '--rm', '--network', 'none', '--entrypoint', 'java', image,
    '-Dloader.main=es.us.dp1.lx_xy_24_25.dixit.configuration.BootstrapAdmin',
    '-cp', '/app/app.jar', 'org.springframework.boot.loader.launch.PropertiesLauncher'], capture_output=True, text=True, timeout=30)
assert bootstrap.returncode != 0 and 'Use an interactive terminal' in bootstrap.stderr

access = json.loads((root / 'target/postgres-completed-game.json').read_text())
with server('first'):
    for path in ('/', '/login', '/register', '/friends', '/games/123/game'):
        assert b'<div id="root"></div>' in request(path)
    html = request('/').decode()
    script = re.search(r'src="(/static/js/[^\"]+)"', html)[1]
    assert len(request(script)) > 100
    assert request('/Cards/0.png').startswith(b'\x89PNG')
    assert request('/ws/info')['websocket'] is True
    request('/api/v1/games/my', expected=401)
    request('/actuator/env', expected=401)
    request('/h2-console/', expected=401)
    request('/static/js/absent.js', expected=404)
    request('/api/v1/games', access['token'], {}, method='POST', expected=503)
    saved = request('/api/v1/games/' + str(access['gameId']), access['token'])
    assert saved['endDate'] and len(saved['turns']) == 8
    # Force real runtime connections to close in the disposable CI service only.
    sql = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE usename='dixit_runtime' AND datname='dixit_ci'"
    subprocess.run(['docker', 'exec', database_container, 'psql', '-U', 'postgres', '-d', 'dixit_ci',
                    '-v', 'ON_ERROR_STOP=1', '-c', sql], check=True, stdout=subprocess.DEVNULL)
    deadline = time.monotonic() + 30
    while True:
        try:
            restored = request('/api/v1/games/' + str(access['gameId']), access['token'])
            assert_history_unchanged(saved, restored)
            break
        except (AssertionError, URLError, TimeoutError):
            if time.monotonic() >= deadline:
                raise
            time.sleep(.5)
    print('Image recovered interrupted PostgreSQL connections and retained completed history', flush=True)
with server('restart'):
    assert_history_unchanged(saved, request('/api/v1/games/' + str(access['gameId']), access['token']))
print('Two real image starts, configurable PORT, routes, API guards, 512 MB limit and graceful stops verified; no active-game recovery claimed', flush=True)

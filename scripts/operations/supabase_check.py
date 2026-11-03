"""Explicit local acceptance on the owner's dedicated Supabase test project.

Never called by CI. Requires protected backend configuration, no API keys.
"""
import argparse
from contextlib import contextmanager
import json
import os
from pathlib import Path
import re
import subprocess
import time
from urllib.error import URLError
from urllib.parse import urlsplit
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[2]


def private_config(path):
    if path.stat().st_mode & 0o077:
        raise SystemExit('Configuration must be private (chmod 600); no values printed.')
    values = {}
    for line in path.read_text().splitlines():
        if not line or line.startswith('#'):
            continue
        key, value = line.split('=', 1)
        if not re.fullmatch(r'(DIXIT_[A-Z_]+|SPRING_PROFILES_ACTIVE|PORT)', key):
            raise SystemExit('Unexpected configuration key; never source this file as shell code.')
        values[key] = value
    return values


class Operator:
    def __init__(self, config, pg_bin, project):
        if os.environ.get('GITHUB_ACTIONS') or os.environ.get('CI', '').lower() == 'true':
            raise SystemExit('Managed-database acceptance is forbidden in ordinary CI.')
        self.values = private_config(config)
        self.pg_bin = pg_bin
        self.url = self.values['DIXIT_DB_URL']
        endpoint = urlsplit(self.url.removeprefix('jdbc:'))
        if endpoint.scheme != 'postgresql' or endpoint.username or endpoint.password:
            raise SystemExit('Use PostgreSQL JDBC URL without embedded credentials.')
        if not re.fullmatch(r'[a-z]{20}', project):
            raise SystemExit('Explicit project reference required.')
        if not (endpoint.hostname == f'db.{project}.supabase.co' or
                (endpoint.hostname and endpoint.hostname.endswith('.pooler.supabase.com') and endpoint.port == 5432
                 and self.values['DIXIT_DB_USERNAME'].endswith('.' + project))):
            raise SystemExit('Endpoint/user does not match the authorized Supabase project/session mode.')
        migration = urlsplit(self.values.get('DIXIT_MIGRATION_DB_URL', self.url).removeprefix('jdbc:'))
        if not (migration.scheme == 'postgresql' and not migration.username and not migration.password and
                (migration.hostname == f'db.{project}.supabase.co' or
                 (migration.hostname and migration.hostname.endswith('.pooler.supabase.com') and migration.port == 5432))):
            raise SystemExit('Migration endpoint does not match the authorized project/session mode.')
        if not (self.values.get('DIXIT_DB_ROLE') == 'dixit_runtime' and
                self.values.get('DIXIT_DB_USERNAME') in ('dixit_runtime', 'dixit_runtime.' + project) and
                self.values.get('DIXIT_MIGRATION_DB_USERNAME') in ('dixit_migrator', 'dixit_migrator.' + project)):
            raise SystemExit('Operator validation requires the separate restricted Dixit roles, never postgres.')
        if self.values.get('DIXIT_MIGRATION_DB_SSL_MODE', self.values.get('DIXIT_DB_SSL_MODE', 'verify-full')) != 'verify-full':
            raise SystemExit('Migration validation requires verify-full.')
        if self.values.get('DIXIT_DB_SSL_MODE', 'verify-full') != 'verify-full':
            raise SystemExit('Managed-database validation requires verify-full.')
        self.env = dict(os.environ, **self.values)
        # Only explicitly selected tests receive this opt-in, never normal PR workflows.
        self.env['DIXIT_TEST_POSTGRES_URL'] = self.url
        self.env['SPRING_PROFILES_ACTIVE'] = 'prod'
        self.env['DIXIT_ACCEPTANCE_GRACE_MS'] = '30000'

    def query(self, sql, runtime=False):
        prefix = 'DIXIT_DB_' if runtime else 'DIXIT_MIGRATION_DB_'
        endpoint = urlsplit(self.values.get(prefix + 'URL', self.url).removeprefix('jdbc:'))
        env = dict(self.env, PGHOST=endpoint.hostname, PGPORT=str(endpoint.port or 5432),
                   PGDATABASE=endpoint.path.lstrip('/'), PGUSER=self.values[prefix + 'USERNAME'],
                   PGPASSWORD=self.values[prefix + 'PASSWORD'], PGSSLMODE='verify-full',
                   PGSSLROOTCERT=self.values.get(prefix + 'SSL_ROOT_CERT', self.values['DIXIT_DB_SSL_ROOT_CERT']),
                   PGCONNECT_TIMEOUT='10', PGAPPNAME='dixit-managed-acceptance')
        # Password is process environment only, never a CLI argument or output.
        result = subprocess.run([str(self.pg_bin / 'psql'), '-X', '-qAt', '-w', '-v', 'ON_ERROR_STOP=1'],
                                input=sql, env=env, text=True, capture_output=True)
        if result.returncode:
            raise RuntimeError('Managed PostgreSQL query failed; no SQL or credentials printed.')
        return result.stdout.strip()

    def run(self, command, log_name):
        target = ROOT / 'target' / log_name
        target.parent.mkdir(exist_ok=True)
        with target.open('w') as log:
            target.chmod(0o600)
            result = subprocess.run(command, env=self.env, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        self.check_log(target)
        if result.returncode:
            raise RuntimeError('Acceptance failed; inspect private ' + log_name)

    def check_log(self, path):
        text = path.read_text()
        assert all(self.values[name] not in text for name in
                   ('DIXIT_DB_PASSWORD', 'DIXIT_MIGRATION_DB_PASSWORD', 'DIXIT_JWT_SECRET'))

    @contextmanager
    def server(self, label):
        jar, = (ROOT / 'target').glob('*.jar')
        path = ROOT / f'target/supabase-{label}.log'
        with path.open('w') as log:
            path.chmod(0o600)
            process = subprocess.Popen(['java', '-jar', str(jar), '--server.address=127.0.0.1', '--server.port=0'],
                                       env=self.env, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
            try:
                deadline = time.monotonic() + 120
                while True:
                    assert process.poll() is None, 'JAR exited; inspect private ' + path.name
                    match = re.search(r'Tomcat started on port (\d+)', path.read_text())
                    if match:
                        try:
                            with urlopen('http://127.0.0.1:' + match[1] + '/actuator/health', timeout=3) as response:
                                assert json.load(response) == {'status': 'UP'}
                            break
                        except (URLError, TimeoutError):
                            pass
                    assert time.monotonic() < deadline, 'Managed startup timeout'
                    time.sleep(.5)
                yield 'http://127.0.0.1:' + match[1]
            finally:
                process.terminate()
                try:
                    process.wait(timeout=30)
                except subprocess.TimeoutExpired:
                    process.kill(); process.wait()
                    raise AssertionError('JAR exceeded graceful stop deadline')
        self.check_log(path)

    def permissions(self):
        summary = json.loads(self.query("""SELECT json_build_object(
          'tables',(SELECT count(*) FROM pg_tables WHERE schemaname='dixit'),
          'migrations',(SELECT json_agg(version ORDER BY installed_rank) FROM dixit.flyway_schema_history WHERE success),
          'images',(SELECT count(*) FROM dixit.images),
          'badges',(SELECT count(*) FROM dixit.badge_images),
          'authorities',(SELECT count(*) FROM dixit.authorities),
          'runtime_create_schema',has_schema_privilege('dixit_runtime','dixit','CREATE'),
          'runtime_create_database',has_database_privilege('dixit_runtime',current_database(),'CREATE'),
          'migration_create_database',has_database_privilege('dixit_migrator',current_database(),'CREATE'),
          'runtime_write_catalog',has_table_privilege('dixit_runtime','dixit.images','UPDATE'),
          'runtime_write_flyway',has_table_privilege('dixit_runtime','dixit.flyway_schema_history','UPDATE'),
          'api_schema_access',(SELECT count(*) FROM pg_roles WHERE rolname IN ('anon','authenticated','service_role') AND has_schema_privilege(oid,'dixit','USAGE')),
          'api_table_access',(SELECT count(*) FROM pg_roles r CROSS JOIN pg_tables t WHERE r.rolname IN ('anon','authenticated','service_role') AND t.schemaname='dixit' AND has_table_privilege(r.oid,format('%I.%I',t.schemaname,t.tablename),'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')),
          'realtime_tables',(SELECT count(*) FROM pg_publication_tables WHERE schemaname='dixit'))"""))
        assert summary['tables'] == 16 and summary['migrations'] == ['1', '2', '3']
        assert (summary['images'], summary['badges'], summary['authorities']) == (36, 12, 2)
        for key in ('runtime_create_schema', 'runtime_create_database', 'migration_create_database',
                    'runtime_write_catalog', 'runtime_write_flyway'):
            assert summary[key] is False, key
        for key in ('api_schema_access', 'api_table_access', 'realtime_tables'):
            assert summary[key] == 0, key
        roles = json.loads(self.query("""SELECT json_agg(json_build_object('name',rolname,'superuser',rolsuper,
          'create_db',rolcreatedb,'create_role',rolcreaterole,'inherit',rolinherit,'replication',rolreplication,'bypass_rls',rolbypassrls))
          FROM pg_roles WHERE rolname IN ('dixit_runtime','dixit_migrator')"""))
        assert len(roles) == 2
        assert all(value is False for role in roles for key, value in role.items() if key != 'name')
        print('Managed permissions verified: ' + json.dumps(summary), flush=True)

    def fingerprint(self):
        tables = self.query("SELECT tablename FROM pg_tables WHERE schemaname='dixit' ORDER BY tablename").splitlines()
        rows = {}
        for table in tables:
            assert re.fullmatch('[a-z_]+', table)
            rows[table] = self.query(f"SELECT count(*)::text||':'||md5(coalesce(string_agg(row_to_json(t)::text,E'\\n' ORDER BY row_to_json(t)::text),'')) FROM dixit.\"{table}\" t")
        rows['sequence'] = self.query('SELECT last_value,is_called FROM dixit.entity_sequence')
        return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--pg-bin', type=Path, required=True)
    parser.add_argument('--project-ref', required=True)
    parser.add_argument('--phase', choices=['migrate', 'acceptance', 'permissions', 'cancellation', 'restart', 'maintenance'], required=True)
    parser.add_argument('--acknowledge-test-data', action='store_true', required=True)
    args = parser.parse_args()
    operator = Operator(args.config, args.pg_bin, args.project_ref)
    assert operator.query("SELECT current_setting('server_version_num')::int / 10000") == '17', 'PostgreSQL major change requires review'
    if args.phase == 'migrate':
        assert operator.query("SELECT count(*) FROM pg_tables WHERE schemaname='dixit'") == '0', 'Refusing migration-first proof on nonempty schema'
        with operator.server('initial-migration'):
            operator.permissions()
            assert operator.query('SELECT count(*) FROM dixit.appusers') == '0'
            assert operator.query('SELECT count(*) FROM dixit.games') == '0'
        print('Fresh managed migration and Hibernate validation passed; no academic accounts/games', flush=True)
    elif args.phase == 'acceptance':
        operator.permissions()
        operator.run(['./mvnw', '--batch-mode', '--no-transfer-progress', '-Dspring.profiles.active=prod',
                      '-Dtest.reports.dir=target/supabase-surefire-reports',
                      '-Dtest=PostgresPersistenceIT,GameActionPermissionsTest,GamePhasePrivacyTest,FriendRelationshipsTest,GameCancellationTest,ChatAccountIdentityTest,StompPermissionsTest',
                      'test'], 'supabase-acceptance.log')
        operator.run(['python3', 'scripts/ci/verify_production.py'], 'supabase-restart-check.log')
        before = operator.fingerprint()
        with operator.server('no-reinsert'):
            assert operator.fingerprint() == before, 'Startup changed persisted rows or sequence'
        operator.permissions()
        print('Managed acceptance/restarts passed; all 16 tables and sequence unchanged on final startup. No active-game recovery or visual claim.', flush=True)
    elif args.phase == 'cancellation':
        operator.run(['./mvnw', '--batch-mode', '--no-transfer-progress', '-Dspring.profiles.active=prod',
                      '-Dtest.reports.dir=target/supabase-cancellation-reports', '-Dtest=GameCancellationTest', 'test'],
                     'supabase-cancellation.log')
        print('Managed cancellation/reconnection cases passed with the actual 30000 ms policy; CI default remains 2000 ms.', flush=True)
    elif args.phase == 'maintenance':
        operator.permissions()
        operator.run(['./mvnw', '--batch-mode', '--no-transfer-progress', '-Dspring.profiles.active=prod',
                      '-Dtest.reports.dir=target/supabase-maintenance-reports',
                      '-Dtest=PostgresPersistenceIT,MaintenancePermissionsTest,MaintenanceTransactionTest', 'test'],
                     'supabase-maintenance.log')
        operator.permissions()
        print('Managed maintenance authorization, admission transactions and complete four-player game passed.', flush=True)
    elif args.phase == 'restart':
        operator.run(['python3', 'scripts/ci/verify_production.py'], 'supabase-restart-check.log')
        before = operator.fingerprint()
        with operator.server('no-reinsert') as base:
            assert operator.fingerprint() == before, 'Startup changed persisted rows or sequence'
            # Select exact own-role PIDs first. Never put a side-effecting function
            # in WHERE: PostgreSQL may reorder its evaluation before other filters.
            pids = operator.query("SELECT pid FROM pg_stat_activity WHERE usename=current_user AND pid<>pg_backend_pid() AND application_name='PostgreSQL JDBC Driver'", runtime=True).splitlines()
            assert pids and all(pid.isdigit() for pid in pids), 'No runtime connection selected'
            for pid in pids:
                assert operator.query('SELECT pg_terminate_backend(' + pid + ')', runtime=True) == 't'
            deadline = time.monotonic() + 30
            while True:
                try:
                    with urlopen(base + '/actuator/health', timeout=5) as response:
                        assert json.load(response) == {'status': 'UP'}
                    break
                except (AssertionError, URLError, TimeoutError):
                    if time.monotonic() >= deadline: raise
                    time.sleep(.5)
            assert operator.fingerprint() == before
        operator.permissions()
        print('Managed pool recovered interrupted runtime connections through session pooling/TLS.', flush=True)
        print('Managed restart retained all 16 tables and sequence without reinsertion.', flush=True)
    else:
        operator.permissions()


if __name__ == '__main__':
    main()

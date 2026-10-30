"""Encrypted owner-run schema backup, isolated PostgreSQL 17 restore and real JAR checks.

No managed database is overwritten. Never run in GitHub Actions.
"""
import argparse
import base64
from datetime import datetime, timezone
import hashlib
import hmac
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import tempfile
from supabase_check import Operator, ROOT


def scram(password):
    salt = secrets.token_bytes(16)
    salted = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 4096)
    client = hmac.new(salted, b'Client Key', 'sha256').digest()
    stored = hashlib.sha256(client).digest()
    server = hmac.new(salted, b'Server Key', 'sha256').digest()
    b64 = lambda value: base64.b64encode(value).decode()
    return 'SCRAM-SHA-256$4096:' + b64(salt) + '$' + b64(stored) + ':' + b64(server)


def protected_write(path, value):
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, 'w') as output:
        output.write(value)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--pg-bin', type=Path, required=True)
    parser.add_argument('--project-ref', required=True)
    parser.add_argument('--backup-dir', type=Path, required=True)
    parser.add_argument('--key-file', type=Path, required=True)
    parser.add_argument('--acknowledge-test-data', action='store_true', required=True)
    args = parser.parse_args()
    operator = Operator(args.config, args.pg_bin, args.project_ref)
    operator.permissions()
    if args.backup_dir.is_symlink() or args.key_file.is_symlink():
        raise SystemExit('Use owner-controlled directories/files, not symlinks.')
    args.backup_dir.mkdir(parents=True, exist_ok=True, mode=0o700)
    assert args.backup_dir.stat().st_mode & 0o077 == 0, 'Backup directory must have mode 700'
    args.key_file.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    if not args.key_file.exists():
        protected_write(args.key_file, secrets.token_urlsafe(48) + '\n')
    assert args.key_file.stat().st_mode & 0o077 == 0, 'Encryption key file must have mode 600'
    before = operator.fingerprint()
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    encrypted = args.backup_dir / (stamp + '-dixit.dump.gpg')
    assert not encrypted.exists()
    # Only the Dixit schema is exported, not Supabase Auth/Storage/internal services.
    from urllib.parse import urlsplit
    endpoint = urlsplit(operator.values.get('DIXIT_MIGRATION_DB_URL', operator.url).removeprefix('jdbc:'))
    remote = dict(operator.env, PGHOST=endpoint.hostname, PGPORT=str(endpoint.port or 5432),
                  PGDATABASE=endpoint.path.lstrip('/'), PGUSER=operator.values['DIXIT_MIGRATION_DB_USERNAME'],
                  PGPASSWORD=operator.values['DIXIT_MIGRATION_DB_PASSWORD'], PGSSLMODE='verify-full',
                  PGSSLROOTCERT=operator.values.get('DIXIT_MIGRATION_DB_SSL_ROOT_CERT', operator.values['DIXIT_DB_SSL_ROOT_CERT']),
                  PGCONNECT_TIMEOUT='10', PGAPPNAME='dixit-operator-backup')
    gpg_home = args.key_file.parent / 'gnupg'
    gpg_home.mkdir(mode=0o700, exist_ok=True)
    gpg = ['gpg', '--no-options', '--homedir', str(gpg_home), '--batch', '--yes', '--pinentry-mode', 'loopback',
           '--passphrase-file', str(args.key_file)]
    with tempfile.TemporaryDirectory(prefix='dixit-restore-') as directory:
        temporary = Path(directory)
        dump = temporary / 'dixit.dump'
        with dump.open('wb') as output:
            dump.chmod(0o600)
            subprocess.run([str(args.pg_bin / 'pg_dump'), '--format=custom', '--schema=dixit', '--no-password'],
                           env=remote, stdout=output, check=True)
        digest = hashlib.sha256(dump.read_bytes()).hexdigest()
        subprocess.run([*gpg, '--cipher-algo', 'AES256', '--symmetric', '--output', str(encrypted), str(dump)],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        encrypted.chmod(0o600)
        dump.unlink()
        # Restore from a verified decryption, not the pre-encryption plaintext.
        subprocess.run([*gpg, '--decrypt', '--output', str(dump), str(encrypted)],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        dump.chmod(0o600)
        assert hashlib.sha256(dump.read_bytes()).hexdigest() == digest, 'Encrypted copy did not round-trip'
        assert operator.fingerprint() == before, 'Managed data changed during backup; retry in maintenance'
        owner_password = secrets.token_hex(32)
        runtime_password = secrets.token_hex(32)
        migration_password = secrets.token_hex(32)
        pwfile = temporary / 'owner.pass'
        protected_write(pwfile, owner_password)
        data = temporary / 'data'
        with (temporary / 'initdb.log').open('w') as log:
            subprocess.run([str(args.pg_bin / 'initdb'), '-D', str(data), '-U', 'dixit_restore_owner',
                            '-A', 'scram-sha-256', '--encoding=UTF8', '--pwfile', str(pwfile), '--no-locale'],
                           check=True, stdout=log, stderr=subprocess.STDOUT)
        pwfile.unlink()
        with socket.socket() as listener:
            listener.bind(('127.0.0.1', 0))
            port = listener.getsockname()[1]
        local = dict(os.environ, PGHOST='127.0.0.1', PGPORT=str(port), PGUSER='dixit_restore_owner',
                     PGDATABASE='postgres', PGPASSWORD=owner_password, PGSSLMODE='disable')
        def query(sql):
            result = subprocess.run([str(args.pg_bin / 'psql'), '-X', '-qAt', '-w', '-v', 'ON_ERROR_STOP=1'],
                                    input=sql, text=True, env=local, capture_output=True)
            assert result.returncode == 0, 'Isolated restore query failed; no values printed'
            return result.stdout.strip()
        started = False
        try:
            subprocess.run([str(args.pg_bin / 'pg_ctl'), '-D', str(data), '-l', str(temporary / 'server.log'),
                            '-o', f'-h 127.0.0.1 -p {port} -k {temporary}', '-w', 'start'],
                           check=True, stdout=subprocess.DEVNULL)
            started = True
            query(f"CREATE ROLE dixit_migrator LOGIN PASSWORD '{scram(migration_password)}';\n"
                  f"CREATE ROLE dixit_runtime LOGIN PASSWORD '{scram(runtime_password)}';\n"
                  'CREATE DATABASE dixit_restore OWNER dixit_migrator;')
            local['PGDATABASE'] = 'dixit_restore'
            subprocess.run([str(args.pg_bin / 'pg_restore'), '--exit-on-error', '--dbname', 'dixit_restore', str(dump)],
                           env=local, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
            tables = query("SELECT tablename FROM pg_tables WHERE schemaname='dixit' ORDER BY tablename").splitlines()
            assert len(tables) == 16
            restored = {}
            for table in tables:
                restored[table] = query(f"SELECT count(*)::text||':'||md5(coalesce(string_agg(row_to_json(t)::text,E'\\n' ORDER BY row_to_json(t)::text),'')) FROM dixit.\"{table}\" t")
            restored['sequence'] = query('SELECT last_value,is_called FROM dixit.entity_sequence')
            assert restored == before, 'Isolated restoration differs from the managed schema'
            print('Encrypted managed backup round-trips; all 16 restored tables, Flyway and sequence match', flush=True)
            env = dict(operator.env)
            url = f'jdbc:postgresql://127.0.0.1:{port}/dixit_restore'
            env.update(DIXIT_DB_URL=url, DIXIT_MIGRATION_DB_URL=url, DIXIT_TEST_POSTGRES_URL=url,
                       DIXIT_DB_USERNAME='dixit_runtime', DIXIT_DB_PASSWORD=runtime_password,
                       DIXIT_MIGRATION_DB_USERNAME='dixit_migrator', DIXIT_MIGRATION_DB_PASSWORD=migration_password,
                       DIXIT_DB_ROLE='dixit_runtime', DIXIT_DB_SSL_MODE='disable', DIXIT_MIGRATION_DB_SSL_MODE='disable')
            log_path = ROOT / 'target/supabase-restored-app.log'
            with log_path.open('w') as log:
                log_path.chmod(0o600)
                result = subprocess.run(['python3', 'scripts/ci/verify_production.py'], env=env, cwd=ROOT,
                                        stdout=log, stderr=subprocess.STDOUT)
            assert result.returncode == 0, 'Restored JAR failed; inspect private supabase-restored-app.log'
            text = log_path.read_text()
            assert all(value not in text for value in (owner_password, runtime_password, migration_password,
                                                       operator.values['DIXIT_JWT_SECRET']))
            print('Restored production JAR/restarts passed on isolated PostgreSQL 17; managed database untouched', flush=True)
        finally:
            if started:
                subprocess.run([str(args.pg_bin / 'pg_ctl'), '-D', str(data), '-m', 'fast', '-w', 'stop'],
                               check=True, stdout=subprocess.DEVNULL)
    metadata = {'created_utc': stamp, 'postgres_major': 17, 'schema': 'dixit', 'tables': 16,
                'encrypted_sha256': hashlib.sha256(encrypted.read_bytes()).hexdigest(),
                'restored_and_app_verified': True}
    protected_write(encrypted.with_suffix(encrypted.suffix + '.json'), json.dumps(metadata, indent=2) + '\n')
    print('Protected encrypted backup retained outside Git: ' + str(encrypted), flush=True)
    print('Owner must keep the encryption key and JWT separately; this run does not schedule periodic backups.', flush=True)


if __name__ == '__main__':
    main()

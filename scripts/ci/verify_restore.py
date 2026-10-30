"""Dump/restore the disposable CI PostgreSQL service, compare all rows, boot restored DB."""
from pathlib import Path
import os
import subprocess
import sys

container = sys.argv[1]
assert os.environ["DIXIT_TEST_POSTGRES_URL"].endswith("/dixit_ci")
backup = Path("target/postgres-backup.dump")
with backup.open("wb") as output:
    subprocess.run(["docker", "exec", container, "pg_dump", "-U", "postgres", "-Fc", "dixit_ci"],
                   stdout=output, check=True)
backup.chmod(0o600)
subprocess.run(["docker", "exec", container, "psql", "-U", "postgres", "-v", "ON_ERROR_STOP=1",
                "-c", "CREATE DATABASE dixit_restore OWNER dixit_migrator"], check=True)
with backup.open("rb") as source:
    subprocess.run(["docker", "exec", "-i", container, "pg_restore", "-U", "postgres",
                    "--exit-on-error", "-d", "dixit_restore"], stdin=source, check=True)

def query(database, sql):
    return subprocess.check_output(["docker", "exec", container, "psql", "-U", "postgres",
                                     "-At", "-v", "ON_ERROR_STOP=1", "-d", database, "-c", sql], text=True).strip()

tables = query("dixit_ci", "select tablename from pg_tables where schemaname='dixit' order by tablename").splitlines()
assert len(tables) == 16, tables  # 15 domain tables plus Flyway history.
for table in tables:
    # Hash rows in a stable order; never print account data or password hashes.
    sql = f"SELECT count(*)::text || ':' || md5(coalesce(string_agg(row_to_json(t)::text,E'\\n' ORDER BY row_to_json(t)::text),'')) FROM dixit.\"{table}\" t"
    assert query("dixit_ci", sql) == query("dixit_restore", sql), f"Restore mismatch: {table}"
sql = "select last_value,is_called from dixit.entity_sequence"
assert query("dixit_ci", sql) == query("dixit_restore", sql), "Sequence state changed"
print(f"Backup/restore matched all {len(tables)} tables, complete rows and sequence", flush=True)
env = dict(os.environ)
for name in ("DIXIT_DB_URL", "DIXIT_TEST_POSTGRES_URL"):
    env[name] = env[name].removesuffix("dixit_ci") + "dixit_restore"
subprocess.run([sys.executable, "scripts/ci/verify_production.py"], env=env, check=True)
backup.unlink()
print("Restored database passed production JAR validation and restart checks; temporary dump removed", flush=True)

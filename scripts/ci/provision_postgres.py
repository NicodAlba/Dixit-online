"""Provision only a fresh, disposable CI service container, never an external DB."""
import base64
import os
import secrets
import subprocess
import sys

container = sys.argv[1]
passwords = {name: secrets.token_hex(32) for name in ("runtime", "migrator")}
# psql receives SQL on stdin; no credentials are passed in process arguments.
sql = "\n".join(
    f"CREATE ROLE dixit_{role} LOGIN PASSWORD '{password}';"
    for role, password in passwords.items()
) + "\nCREATE DATABASE dixit_ci OWNER dixit_migrator;\n"
subprocess.run(["docker", "exec", "-i", container, "psql", "-U", "postgres",
                "-v", "ON_ERROR_STOP=1"], input=sql, text=True, check=True)
values = {
    "DIXIT_DB_URL": "jdbc:postgresql://127.0.0.1:5432/dixit_ci",
    "DIXIT_TEST_POSTGRES_URL": "jdbc:postgresql://127.0.0.1:5432/dixit_ci",
    "DIXIT_DB_USERNAME": "dixit_runtime",
    "DIXIT_DB_PASSWORD": passwords["runtime"],
    "DIXIT_MIGRATION_DB_USERNAME": "dixit_migrator",
    "DIXIT_MIGRATION_DB_PASSWORD": passwords["migrator"],
    "DIXIT_JWT_SECRET": base64.b64encode(secrets.token_bytes(64)).decode(),
    # Only this loopback CI database disables TLS. Production defaults to verify-full.
    "DIXIT_DB_SSL_MODE": "disable",
    "DIXIT_ALLOWED_ORIGINS": "http://127.0.0.1:18080",
}
for name in ("DIXIT_DB_PASSWORD", "DIXIT_MIGRATION_DB_PASSWORD", "DIXIT_JWT_SECRET"):
    print("::add-mask::" + values[name])
with open(os.environ["GITHUB_ENV"], "a") as env:
    for name, value in values.items():
        env.write(f"{name}={value}\n")

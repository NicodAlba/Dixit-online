"""Production JAR on a disposable PostgreSQL database; no external deployment."""
from pathlib import Path
import contextlib
import json
import os
import re
import secrets
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from zipfile import ZipFile

root = Path(__file__).resolve().parents[2]
jar, = (root / "target").glob("*.jar")
with ZipFile(jar) as archive:
    assert "BOOT-INF/classes/data.sql" not in archive.namelist(), "Academic accounts bundled"
    assert b"spring.profiles.default=prod" in archive.read("BOOT-INF/classes/application.properties")
    assert "BOOT-INF/classes/application-mysql.properties" not in archive.namelist()
    assert "BOOT-INF/classes/static/index.html" in archive.namelist()
    assert "BOOT-INF/lib/postgresql-42.7.14.jar" in archive.namelist()
    for version in ("V1__initial_schema.sql", "V2__reference_catalogs.sql", "V3__runtime_privileges.sql"):
        assert "BOOT-INF/classes/db/migration/" + version in archive.namelist()
assert os.environ.get("DIXIT_TEST_POSTGRES_URL"), "Requires an explicitly disposable test database"
assert os.environ["DIXIT_DB_URL"] == os.environ["DIXIT_TEST_POSTGRES_URL"]

# Prove a missing key fails before an actual database connection, with no ephemeral fallback.
invalid_env = {key: value for key, value in os.environ.items() if not key.startswith("SPRING_")}
invalid_env.pop("DIXIT_JWT_SECRET", None)
with (root / "target/production-missing-secret.log").open("w") as log:
    result = subprocess.run(["java", "-jar", str(jar), "--spring.profiles.active=prod"],
                            env=invalid_env, stdout=log, stderr=subprocess.STDOUT, timeout=30)
assert result.returncode != 0
failure = (root / "target/production-missing-secret.log").read_text()
assert "dp1.game.app.jwtSecret" in failure and "Using an ephemeral" not in failure
print("Production JAR excludes academic seeds and rejects missing JWT secret", flush=True)

@contextlib.contextmanager
def server(label):
    log_path = root / f"target/production-{label}.log"
    with log_path.open("w") as log:
        process = subprocess.Popen(["java", "-jar", str(jar), "--spring.profiles.active=prod",
                                    "--server.address=127.0.0.1", "--server.port=0"],
                                   stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 90
            while True:
                assert process.poll() is None, f"Server exited; inspect {log_path.name}"
                match = re.search(r"Tomcat started on port (\d+)", log_path.read_text())
                if match:
                    base = "http://127.0.0.1:" + match[1]
                    try:
                        with urlopen(base + "/actuator/health", timeout=2) as response:
                            assert json.load(response) == {"status": "UP"}
                        break
                    except (URLError, TimeoutError):
                        pass
                assert time.monotonic() < deadline, f"Startup timeout; inspect {log_path.name}"
                time.sleep(.5)
            yield base
        finally:
            process.terminate()
            try:
                process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                process.kill(); process.wait()
                raise AssertionError("Graceful shutdown exceeded 30s")
    # A test secret must never be echoed in normal or startup diagnostics.
    for name in ("DIXIT_DB_PASSWORD", "DIXIT_MIGRATION_DB_PASSWORD", "DIXIT_JWT_SECRET"):
        assert os.environ[name] not in log_path.read_text()


def request(base, path, token=None, body=None, method="GET", expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode()
    req = Request(base + path, data=data, headers=headers, method=method)
    try:
        with urlopen(req, timeout=10) as response:
            assert response.status == expected
            content = response.read()
            return json.loads(content) if response.headers.get_content_type() == "application/json" else content
    except HTTPError as error:
        assert error.code == expected, (path, error.code, expected)
        return None

history_access = json.loads((root / "target/postgres-completed-game.json").read_text())
name = "restart_" + secrets.token_hex(8)
password = secrets.token_urlsafe(24)
with server("first") as base:
    for path in ("/", "/login", "/register", "/friends", "/games/123/game", "/users/123/achievements"):
        assert b'<div id="root"></div>' in request(base, path)
    assert request(base, "/ws/info")["websocket"] is True
    for origin, expected in ((os.environ["DIXIT_ALLOWED_ORIGINS"].split(",")[0], 200), ("https://untrusted.example", 403)):
        req = Request(base + "/ws/info", headers={"Origin": origin})
        try:
            with urlopen(req, timeout=5) as response:
                assert response.status == expected
        except HTTPError as error:
            assert error.code == expected
    request(base, "/api/v1/games/my", expected=401)
    request(base, "/actuator/env", expected=401)
    request(base, "/h2-console/", expected=401)
    request(base, "/static/js/absent.js", expected=404)
    request(base, "/api/v1/absent", expected=401)
    request(base, "/api/v1/auth/signup", body={"username": name, "password": password, "authority": "PLAYER"}, method="POST", expected=201)
    login = request(base, "/api/v1/auth/signin", body={"username": name, "password": password}, method="POST")
    token = login["token"]
    lobby = request(base, "/api/v1/games", token, body={}, method="POST", expected=201)
    saved = request(base, "/api/v1/games/" + str(lobby["id"]), token)
    history = request(base, "/api/v1/games/" + str(history_access["gameId"]), history_access["token"])
    assert history["endDate"] and len(history["turns"]) == 8
with server("restart") as base:
    login = request(base, "/api/v1/auth/signin", body={"username": name, "password": password}, method="POST")
    assert login["token"]
    assert request(base, "/api/v1/games/" + str(lobby["id"]), token) == saved
    assert request(base, "/api/v1/games/" + str(history_access["gameId"]), history_access["token"]) == history
    print("PostgreSQL restart retained account, JWT key, completed 8-turn history and unchanged lobby; no active-game recovery claimed", flush=True)
print("Production routes, health, origins, API guards and two real JAR starts verified", flush=True)

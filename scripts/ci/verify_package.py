"""Check the actual Boot JAR, then smoke-test it with disposable H2 on loopback.

No production credentials, external database, browser or deployment is involved.
"""
from pathlib import Path
import json
import os
import re
import socket
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.request import urlopen
from zipfile import ZipFile

root = Path(__file__).resolve().parents[2]
jars = list((root / "target").glob("*.jar"))
assert len(jars) == 1, f"Expected one executable JAR, found {jars}"
build = root / "frontend/build"
assert (build / "index.html").is_file(), "Build React before packaging"
with ZipFile(jars[0]) as jar:
    manifest = jar.read("META-INF/MANIFEST.MF").decode()
    assert "Main-Class: org.springframework.boot.loader.launch.JarLauncher" in manifest
    assert "Start-Class:" in manifest
    assert any(name.startswith("BOOT-INF/lib/") for name in jar.namelist())
    files = [p for p in build.rglob("*") if p.is_file()]
    for file in files:
        name = "BOOT-INF/classes/static/" + file.relative_to(build).as_posix()
        assert jar.read(name) == file.read_bytes(), f"Missing or stale resource: {name}"
print(f"Executable JAR includes all {len(files)} React resources", flush=True)

with socket.socket() as listener:
    listener.bind(("127.0.0.1", 0))
    port = listener.getsockname()[1]
base = f"http://127.0.0.1:{port}"
# Do not inherit deployment configuration or frontend secrets from the caller.
env = {k: v for k, v in os.environ.items()
       if not k.startswith(("DIXIT_", "SPRING_", "REACT_APP_"))}
log_path = root / "target/ci-smoke.log"
with log_path.open("w") as log:
    process = subprocess.Popen([
        "java", "-jar", str(jars[0]), "--server.address=127.0.0.1",
        f"--server.port={port}", "--spring.datasource.url=jdbc:h2:mem:ci_package",
        "--spring.h2.console.enabled=false", "--spring.jpa.show-sql=false",
        "--logging.level.org.hibernate.SQL=WARN",
    ], cwd=root, env=env, stdout=log, stderr=subprocess.STDOUT)
    try:
        deadline = time.monotonic() + 90
        while True:
            assert process.poll() is None, f"JAR exited early; inspect {log_path}"
            try:
                with urlopen(base + "/ws/info", timeout=2) as response:
                    assert response.status == 200
                    assert json.load(response)["websocket"] is True
                break
            except (URLError, TimeoutError):
                assert time.monotonic() < deadline, f"Startup timeout; inspect {log_path}"
                time.sleep(0.5)
        with urlopen(base + "/", timeout=5) as response:
            html = response.read().decode()
            assert response.status == 200 and '<div id="root"></div>' in html
        asset = re.search(r'src="(/static/js/[^\"]+)"', html)
        assert asset, "React entry script missing"
        with urlopen(base + asset[1], timeout=5) as response:
            assert response.status == 200 and len(response.read()) > 100
        for resource in ("/Cards/0.png", "/Achievements/WinAchievements/Win1.png"):
            with urlopen(base + resource, timeout=5) as response:
                assert response.status == 200 and response.read().startswith(b"\x89PNG")
        try:
            urlopen(base + "/api/v1/games/my", timeout=5)
            raise AssertionError("Private API unexpectedly public")
        except HTTPError as error:
            assert error.code == 401, error.code
        print("JAR started: React root/script, SockJS info and private REST guard verified", flush=True)
    finally:
        process.terminate()
        try:
            code = process.wait(timeout=30)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
            raise AssertionError("JAR did not stop within 30 seconds")
        print(f"JAR stopped after SIGTERM (exit {code}); no recovery guarantee tested", flush=True)

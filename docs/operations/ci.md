# CI and executable packaging

The `CI / verify` check runs on pull requests to `develop` and pushes to `develop`.
It does not deploy, publish images, release or access production credentials.

## Local reproduction

Use Java 21, Python 3 and Node from `frontend/.nvmrc` (for example `nvm use`
inside `frontend`). The checked-in Maven wrapper selects Maven 3.9.7.

From the repository root, in a clean checkout:

```sh
cd frontend
npm ci --no-audit --no-fund
CI=true npm test -- --watchAll=false --runInBand
CI=false npm run build
cd ..
SPRING_PROFILES_ACTIVE=test ./mvnw --batch-mode --no-transfer-progress clean verify
python3 scripts/ci/verify_package.py
git diff --exit-code
```

CRA's production build keeps inherited lint and source-map warnings visible.
Only that step uses `CI=false`, so those warnings do not fail the build; test
failures and bundling errors still fail. No dependency upgrade or lockfile
regeneration is part of this workflow. Node 22 is a transitional supported LTS
runtime for this existing CRA toolchain; review it before its April 2027 EOL.

Maven copies the previously built React output into `BOOT-INF/classes/static`.
The package check compares **every** frontend build file byte-for-byte with the
executable JAR and checks its launcher and libraries. It then starts that JAR
on loopback with a disposable H2 database, checks the React root, entry
script, card and badge files, SockJS discovery and a private REST denial, and terminates the process.
This checks packaging and startup, not public proxy transport, visual behavior,
production PostgreSQL compatibility or recovery of active games after restart.

The ordinary JAR uses development defaults and academic sample data. The separate
`production-package` profile excludes those seeds and selects strict production
defaults; see below. Neither artifact is approval for a public deployment.

## PostgreSQL and production package

The same CI job also provisions a disposable PostgreSQL 17.11 service with random
runtime/migration credentials, applies Flyway and runs the explicit PostgreSQL
acceptance suite. It packages without academic seeds, validates two production
JAR starts, then dumps/restores to another isolated database and checks every
row, sequence and the restored application. This is local CI infrastructure,
not a hosted Dixit deployment. See [production.md](production.md) for reproduction.
No tokens/fixture access files, dumps or credentials are uploaded with diagnostics.

## Reports and security

Only failed runs upload Surefire/JaCoCo and smoke/startup diagnostics, retained
for one day; successful runs keep their normal Actions logs without an artifact upload. Logs may contain synthetic test data and must never be generated
against real accounts. JARs and frontend bundles are not published by this CI.
The job has `contents: read`, no persisted checkout credentials and no deployment
secrets. Actions are pinned to commits. Maven/npm caches contain dependencies,
not configuration files, tokens or build outputs. Superseded runs are cancelled.

Historical academic deliverables under `docs/deliverables` are never regenerated
by the default build. To explicitly produce fresh academic reports:

```sh
./mvnw --batch-mode --no-transfer-progress verify -Pacademic-reports
```

They are written under `target/site` and `target/academic`, not over the archived
files. JaCoCo and Surefire reports remain available during ordinary CI without
installing the Allure reporting CLI.

Changes follow `feature/*` or `fix/*` → PR → `develop`. Only the owner promotes
`develop` to `main`. CI success is not deployment approval.

## Inspect runs in GitHub

Open [Actions](https://github.com/NicodAlba/Dixit-online/actions), select CI, open
a run and then `verify`. Check its branch and commit before interpreting results.
Expand each step to read live or completed logs. Green means required checks
passed, red identifies a failed step, yellow is queued/running, and cancelled
can mean a newer commit superseded the run. PR checks also link to these logs.
A green result is not a visual acceptance or a deployment.

## Actual Docker artifact

After the existing tests, packaging and isolated restore, CI builds `dixit:ci`
from Dockerfile in its checkout. It does not push the image anywhere.
`verify_image.py` extracts the final-stage JAR and compares all React resources,
checks strict prod defaults/no academic seeds, UID 10001 and absence of build
tools. It proves missing configuration fails with networking disabled, then
starts the image twice against the disposable PostgreSQL service on host
loopback, with synthetic secrets injected at runtime. It tests PORT, health,
React routes/assets, SockJS info, API guards, completed-history persistence,
recovery after terminating runtime DB connections, and graceful shutdown.
Containers run with a read-only filesystem, writable temporary /tmp, dropped
capabilities and a 512 MB memory limit. This is not a load/capacity guarantee.
Docker is required for that step; a successful local JAR is not image evidence.

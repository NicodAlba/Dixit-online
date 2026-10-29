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
./mvnw --batch-mode --no-transfer-progress clean verify
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

The current JAR retains development defaults and academic sample data. It must
not be deployed publicly until production configuration and data initialization
are prepared and approved. Deep-link routing and public asset authorization
must also be checked in the production preparation delivery.

## Reports and security

GitHub retains only Surefire reports, JaCoCo output and the local smoke log for
seven days. Logs may contain synthetic test data and must never be generated
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

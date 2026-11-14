# Local development and verification

## Prerequisites and isolation

Java 21; Node **22.23.4** as pinned in `frontend/.nvmrc`; npm compatible with that Node distribution; Git. The Maven wrapper selects Maven 3.9.7. Python 3 and Docker are also needed for the full CI packaging/PostgreSQL/image checks. Do not use production secrets or a Supabase URL for ordinary development or tests.

The commands below match the checked-in scripts and workflow. Documentation inspection is not a fresh successful execution of every command; the Phase 6 verification record states what was actually run.

## Two-process development

Run `./mvnw spring-boot:run` from the repository root. In another terminal, enter `frontend`, select the pinned Node version, run `npm ci --no-audit --no-fund`, then `npm start`. React runs at http://localhost:3000 and proxies API traffic to the backend at http://localhost:8080.

The default backend uses H2/create-drop and academic sample data. Treat accounts and games as disposable. The H2 console is disabled unless explicitly enabled for local debugging. Without a configured private JWT signing key, development sessions do not survive backend restarts. Register an ordinary player through the UI; do not reuse academic credentials on a public service.

## Tests and packaged application

From the repository root:

```sh
cd frontend
CI=true npm test -- --watchAll=false --runInBand
CI=false npm run build
cd ..
SPRING_PROFILES_ACTIVE=test ./mvnw --batch-mode --no-transfer-progress clean verify
python3 scripts/ci/verify_package.py
```

Install first with `npm ci` if dependencies are absent. `CI=false` applies only to the inherited CRA build-warning policy; tests still fail on failures. Build React before Maven packaging: Maven copies its output into the executable JAR. Maven alone is not proof that the current React bundle is packaged.

For a manual local packaged launch, after building both parts:

```sh
java -jar target/*.jar
```

Use this wildcard only when exactly one executable JAR is present; otherwise name that JAR explicitly. Open http://localhost:8080. `/docs` is the React API viewer and `/v3/api-docs` is its OpenAPI document. Business endpoint authorization remains enforced independently of public documentation.

## Production boundary

Use [production configuration](operations/production.md), [Docker](operations/docker.md) and [Supabase](operations/supabase.md) for the separate production profile. The production package excludes academic seeds. Flyway applies versioned migrations; Hibernate validates the schema. Runtime and migration credentials are separate, with TLS verification and an external stable JWT key.

Environment examples are templates, not credentials. Store real values outside Git and never in React variables or Docker build arguments. Do not launch another application against the managed demo database for an experiment. CI provisions isolated PostgreSQL and does not need the hosted credentials.

For PostgreSQL, restoration and Docker verification, follow [the existing CI guide](operations/ci.md) and workflow; these checks require additional isolated infrastructure. An H2 test run is not PostgreSQL compatibility evidence.

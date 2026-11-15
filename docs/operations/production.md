# Production configuration and local verification

The hosted demo uses one Spring Boot application and managed PostgreSQL 17. For provider operations, see [Render](render-free.md) and [Supabase](supabase.md). CI does not deploy automatically.

## Configuration

Use Java 21, the version in `frontend/.nvmrc`, the Maven wrapper and PostgreSQL 17. Initial verification used 17.11. Spring Boot 3.5.5 manages Flyway 11.7.2. pgJDBC was pinned to 42.7.14 for the [driver security fixes](https://jdbc.postgresql.org/security/).

`.env.prod.example` documents placeholders. Supply actual values through the environment or a secret manager. Spring does not load the example file. Keep credentials out of Git, build layers, frontend code and logs.

| Variable | Purpose |
| --- | --- |
| `DIXIT_DB_URL` | PostgreSQL JDBC URL without embedded credentials |
| `DIXIT_DB_USERNAME`, `DIXIT_DB_PASSWORD` | Application connection credentials |
| `DIXIT_DB_ROLE` | Bare SQL role name, which can differ from the pooler username |
| `DIXIT_MIGRATION_DB_USERNAME`, `DIXIT_MIGRATION_DB_PASSWORD` | Separate DDL role credentials |
| `DIXIT_MIGRATION_DB_URL` | Optional Flyway endpoint. Defaults to `DIXIT_DB_URL` |
| `DIXIT_DB_SSL_MODE`, `DIXIT_DB_SSL_ROOT_CERT` | Default `verify-full` and readable CA path, defaulting to `~/.postgresql/root.crt` |
| `DIXIT_MIGRATION_DB_SSL_MODE`, `DIXIT_MIGRATION_DB_SSL_ROOT_CERT` | Optional Flyway TLS overrides |
| `DIXIT_JWT_SECRET` | Stable Base64 key made from at least 64 private random bytes. Store it separately from dumps |
| `DIXIT_ALLOWED_ORIGINS` | Exact comma-separated HTTPS origins without trailing slashes or wildcards. Loopback HTTP is for isolated tests |
| `DIXIT_TRUSTED_PROXY_REGEX` | Confirmed proxy addresses. Defaults to loopback |
| `PORT` | Application port, default 8080 |

Create two private LOGIN roles. The migrator owns the `dixit` schema. The runtime role needs connection and application privileges granted by Flyway, with no schema ownership, DDL, superuser access or catalog writes. Keep the Data API disabled and revoke public API access to the schema. Run CI only against isolated databases.

## Build and launch

Run the checks in [ci.md](ci.md) and build React first. From the root:

```sh
./mvnw --batch-mode --no-transfer-progress -DskipTests -Pproduction-package package
java -jar target/LI-02-2425-dixit-0.1.0.BUILD-SNAPSHOT.jar --spring.profiles.active=prod
```

The production Maven profile excludes `data.sql` and the original MySQL profile. The production JAR defaults to `prod`. Startup validates configuration, runs Flyway V1–V3 and validates JPA mappings. A migration failure blocks startup. Avoid bypassing it with baseline-on-migrate, Flyway clean or ddl-auto=update. The normal package defaults to dev and CI uses test-only resources.

Spring serves allowlisted SPA routes and keeps REST, SockJS and management routes separate. REST and STOMP enforce access to actions and private views. Health reports only status. The H2 console stays disabled. The API documentation is available through the Docs route configured for the demo.

SIGTERM allows up to 20 seconds for pending requests. Planned maintenance drains active games before shutdown. See ADR-0009 for interrupted-game cancellation and the [Render runbook](render-free.md) for deployment.

## PostgreSQL and artifact verification

CI runs isolated PostgreSQL 17.11 with temporary roles. Coverage includes REST/STOMP, permissions, concurrency, persistence and a four-player/eight-turn game. `PostgresPersistenceIT` checks 52 occurrences, 32 historical cards with authors, 24 votes, 20 cards in hands and statistics and achievements.

To repeat locally, use a disposable database and set `DIXIT_TEST_POSTGRES_URL` to the same value as `DIXIT_DB_URL`. Supply credentials privately.

```sh
./mvnw --batch-mode --no-transfer-progress -Dspring.profiles.active=prod \
  -Dtest.reports.dir=target/postgres-surefire-reports \
  -Dtest=PostgresPersistenceIT,GameActionPermissionsTest,GamePhasePrivacyTest,FriendRelationshipsTest,GameCancellationTest,ChatAccountIdentityTest,StompPermissionsTest test
./mvnw --batch-mode --no-transfer-progress -DskipTests -Pproduction-package package
python3 scripts/ci/verify_production.py
```

The smoke check covers seed exclusion, missing-key startup failure, health, SPA routing, API fallback boundaries, SockJS origins and persistence across JAR restarts. Fixture tokens stay under `target`. It does not test visual acceptance, external networks or continuity of active games.

## Updates, rollback and backups

Use the [Render runbook](render-free.md) for admission control, CI-backed manual publication and process replacement. Prefer additive schema changes. Application rollback leaves persisted data and migrations in place. If versions are incompatible, fix forward or verify a restore in a separate database. Never use Flyway clean to roll back a public service.

Create a custom-format TLS pg_dump before changes and after demo sessions. Encrypt it and verify pg_restore against separate PostgreSQL of the same major version. Keep seven verified copies initially and agree recovery targets with the owner. Use protected environment or pgpass credentials. CI verifies restore on synthetic data.

A stable key preserves JWT sessions across restarts. Password changes do not revoke all JWTs immediately. Key rotation invalidates existing tokens. The [maintenance runbook](demo-maintenance.md) describes the interactive, restricted first-admin bootstrap with hidden password entry, BCrypt and transaction locking.

## Render instance guard

Set `DIXIT_SINGLE_INSTANCE_GUARD=true`. The guard claims ownership before Flyway, JPA and recovery and applies to each physical Hikari connection. A competing process gets SQLSTATE 55000. Use a direct connection or session pooler, with a pool of one idle to five maximum connections. See ADR-0011.

Use `DIXIT_RECOVER_INTERRUPTED_GAMES=true` and `DIXIT_MAINTENANCE_ENABLED=false` for ordinary startup. Health and admissions wait for recovery to finish. Setting maintenance to true deliberately starts closed. Follow the stop-first replacement sequence even with the guard. Saving configuration and resuming an old deployment does not apply the new configuration to that artifact.

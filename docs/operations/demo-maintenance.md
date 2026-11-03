# Demo maintenance and first administrator

Status: owner explicitly approved this contract and the local administrator
bootstrap on 2026-10-30, following Phase 5 section 11. One playable application instance; no public deployment or CD
authorized. Supabase Free is selected; Render Free remains an application-host
candidate only.

## Approved behavior

An ADMIN-only REST control changes maintenance mode without restarting Spring.
GET and PUT /api/v1/operations/maintenance return enabled state and counts of
active games and waiting lobbies, without participant details. PUT accepts only
an enabled boolean. Authentication and ADMIN authorization apply on the backend.
There is no anonymous switch, URL token or frontend Supabase credential.

While enabled, backend game creation and initial game start return HTTP 503 with
the English message “New games are temporarily unavailable for maintenance.”
Existing lobbies and invitations remain stored; joining them does not bypass
the start restriction. Active turns, votes, replenishment, chat, history, social
features, authorized subscriptions and reconnection retain their existing rules.
No game is cancelled or awarded because maintenance was enabled.

Activation waits for already admitted creation/start transactions to complete;
new attempts cannot race past the maintenance switch. In-flight requests that
were admitted before activation may commit and appear in the final active count.
The operator must wait for activeGameCount=0 before SIGTERM; waiting lobbies are
reported separately and survive the update. No automatic timeout forces games
to end. Existing host departure/cancellation behavior remains unchanged.

Initial mode is configurable by DIXIT_MAINTENANCE_ENABLED. Set true explicitly
for a replacement process, perform health and functional checks, then reopen
through the authenticated ADMIN endpoint. The switch is per process and is not
a database migration. Operations must never overlap two playable processes.
The local first-administrator bootstrap uses hidden confirmed input, BCrypt and
a transaction-scoped advisory lock. It refuses an existing ADMIN and uses the
restricted runtime database role. It has no Spring bean or public endpoint.

## Manual update sequence

1. Select the approved commit with real CI including its actual image.
2. Enable maintenance in the current process and wait until active games finish.
3. Take and verify the protected backup; never put it in Git or public artifacts.
4. Stop the old process and verify termination before starting its replacement.
5. Start the replacement with maintenance initially true, approved secrets, TLS
   and the single managed database. Apply only reviewed versioned migrations.
6. Check health, routes, login and authorized REST/STOMP through HTTPS/proxy.
7. Reopen using the ADMIN control. Roll back only to an app/schema-compatible
   artifact; reverting code does not undo destructive data changes.

## Unexpected crash remains a separate decision

Accounts, cards, authors, votes, scores and history persist in PostgreSQL.
Broker, presence, WebSocket sessions and host-disconnection deadlines are not
reconstructed automatically. A free host can stop/restart unexpectedly; there
is no promise of continuity or automatic recovery.

After an unexpected crash, reopen in maintenance and inspect active records.
Do not silently cancel or rewrite them at startup. The owner must separately
agree whether to implement explicit supervised cancellation/recovery or scope
automatic reconciliation. Existing actor-authorized exit/cancellation remains
available where applicable. This unresolved policy blocks first publication,
not the independent Docker/database checks.

## Operator commands

Use the actual packaged production JAR and a mode-600 backend configuration file.
Run from a terminal; do not put administrator passwords in arguments or chat:

```sh
python3 scripts/operations/bootstrap_admin.py --config /private/backend.env --jar target/dixit.jar
python3 scripts/operations/maintenance.py --url https://APPROVED-APP-ORIGIN --status
python3 scripts/operations/maintenance.py --url https://APPROVED-APP-ORIGIN --enable
python3 scripts/operations/maintenance.py --url https://APPROVED-APP-ORIGIN --disable
```

Replace the JAR name with the actual packaged filename. Bootstrap runs Java 21
with the packaged PropertiesLauncher. It requires migrated PostgreSQL, checks
that the connecting role lacks administrative/DDL powers and creates only the
first ADMIN atomically. Passwords must match, have at least 12 characters and fit
BCrypt's 72 UTF-8 bytes. Failures print controlled messages/SQL state, not SQL or
credentials. Repetition does not change existing accounts. There is no default
administrator password.

The maintenance helper signs in through Dixit REST; JWT exists only in process
memory. It accepts HTTPS origins or loopback HTTP for local operation. The flag
is in memory: explicitly set DIXIT_MAINTENANCE_ENABLED=true in the host environment
so every replacement/cold start stays closed until an ADMIN reopens it. A Free
host restart can therefore require manual reopening. This is an operational
tradeoff, not automatic availability.

Managed acceptance currently contains identifiable fictitious ADMIN accounts.
They must be reviewed and retired with owner authorization before using the
first-admin bootstrap there. No owner administrator has been invented or created.
No bulk deletion is performed by either helper.

## Verification scope

Backend tests cover ADMIN authorization, strict boolean payloads, creation/start
503, preserved actor authorization and lobby data, admission-versus-commit and
rollback, and closed startup. The PostgreSQL four-player acceptance finishes all
eight rounds while maintenance is enabled. Bootstrap has a separate disposable
PostgreSQL database for competing attempts and verifies one BCrypt account and
no overwrite; ordinary CI never bootstraps a managed database. The real image
check starts closed and verifies create denial, readable history and packaged
CLI rejection without a terminal. Actual results are recorded in CHANGELOG_DEV.
No hosted single-process replacement or public proxy has been executed.

Route: feature/demo-maintenance → PR → develop. No main or public publication.

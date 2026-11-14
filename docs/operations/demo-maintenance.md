# Demo maintenance and first administrator

Status: maintenance/bootstrap approved 2026-10-30. On 2026-11-03 the owner also
authorized a manual occasional demo on Render Free and minimal crash cancellation
(ADR-0009). Exposure is blocked by the gates in render-free.md; no demo is live.

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
only for an intentionally closed, supervised start, then reopen through ADMIN.
Ordinary demo/cold starts must use false and opt in to restart recovery (ADR-0009). The switch is per process and is not
a database migration. Operations must never overlap two playable processes.
The local first-administrator bootstrap uses hidden confirmed input, BCrypt and
a transaction-scoped advisory lock. It refuses an existing ADMIN and uses the
restricted runtime database role. It has no Spring bean or public endpoint.

## Manual update sequence

1. Select the approved commit with real CI including its actual image.
2. Enable maintenance in the current process and wait until active games finish.
3. Take and verify the protected backup; never put it in Git or public artifacts.
4. Stop the old process and verify termination before starting its replacement.
5. Start one replacement with approved secrets/TLS, restart recovery true and
   ordinary maintenance false. Recovery keeps health unavailable until complete.
   Apply only reviewed versioned migrations; planned draining leaves no active games.
6. Check health, routes, login and authorized REST/STOMP through HTTPS/proxy.
7. Health gates ordinary reopening; if explicitly started closed, ADMIN reopens. Roll back only to an app/schema-compatible
   artifact; reverting code does not undo destructive data changes.

## Approved unexpected-crash policy

The owner accepts interruption of active games, without full recovery or HA.
With DIXIT_RECOVER_INTERRUPTED_GAMES=true, startup reuses cancellation under write
locks, preserving cards/turns/authors/votes/points/accounts/relations, with no new
wins or rewards. Completed games and lobbies are excluded; repetition is idempotent.
Recovery failure fails startup and stays closed/unhealthy. Existing English UI
reports cancellation and preserved history. This assumes the old process has
stopped; it is not multi-instance coordination. See ADR-0009 and render-free.md.

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
is in memory. Ordinary demo/cold starts use DIXIT_MAINTENANCE_ENABLED=false and
DIXIT_RECOVER_INTERRUPTED_GAMES=true, so normal restarts do not await an absent
ADMIN. Explicit true remains a deliberate closed-start override; do not leave it
configured for the ordinary demo. Never overlap playable application instances.

On 2026-11-03 the owner authorized downgrading exactly six privately inventoried
fictitious ADMIN accounts to PLAYER after a restored encrypted backup. All six
changes were verified without deleting accounts/history or changing passwords.
The owner then completed hidden terminal bootstrap; a read-only check confirmed
one owner ADMIN and no synthetic ADMIN. No bulk deletion is performed by either
helper. See DEV-064; do not repeat bootstrap or broaden retirement authorization.

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

Route: feature/demo-maintenance → PR → develop. No main changes; public exposure remains gated in render-free.md.

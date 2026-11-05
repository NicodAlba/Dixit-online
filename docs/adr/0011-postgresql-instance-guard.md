# ADR-0011 — Fence runtime PostgreSQL sessions by application process

Status: approved by the owner on 2026-11-04; implemented and verified locally,
in CI, on Supabase session pooling and on actual Render B/C processes on 2026-11-05.
The original unguarded A remains suspended.

## Context

Render can overlap old and replacement stateless processes during provider
maintenance, including Free maintenance without notice. Manual stop-first updates
(ADR-0010) cannot control that path. Startup recovery must never cancel games
while a previous engine can still access their database. Budget remains 0 EUR;
no external broker, Redis, schema or authentication changes are authorized.

Sources checked 2026-11-04:
- https://render.com/docs/platform-maintenance
- https://www.postgresql.org/docs/17/explicit-locking.html#ADVISORY-LOCKS
- https://supabase.com/docs/guides/database/connecting-to-postgres

## Decision

With DIXIT_SINGLE_INSTANCE_GUARD=true, each runtime Hikari physical connection
registers the process UUID in application_name and holds a shared PostgreSQL
session advisory lock. A separate transaction advisory mutex serializes checking
existing cohort holders and registration. Another process UUID is rejected with
SQLSTATE 55000. Registration clears statistics snapshots before inspecting live
locks and session identities; unknown identities fail closed. Minimum idle is
one, maximum pool size remains five. No additional dedicated connection.

The datasource postprocessor claims its first connection before returning the
bean for Flyway/JPA/recovery consumption. Flyway retains its separate restricted
migration role; manual schema updates still require old service shutdown first.
Every replacement physical connection executes the initializer again. Returning
logical pooled connections does not release session locks. Closing one physical
connection leaves other cohort holders protecting their transactions. If all
old sessions are lost, a replacement can claim; reconnecting old sessions then
fail while the replacement holds the cohort. No application tables are changed.

Use only direct PostgreSQL or the approved Supabase **session** pooler on 5432,
with existing verify-full TLS. Transaction pooling is incompatible. The flag is
opt-in for existing independent diagnostics/bootstrap and isolated test tools;
it is required for the next Render application service. Do not switch it on by
rolling-redeploying an unguarded live predecessor: stop that service first.

## Consequences and limits

This fences database access, not physical JVM existence or Render routing.
A provider replacement can fail while its predecessor still owns sessions;
manual stop/retry and an availability interruption may be needed. It does not
recover timers, presence or active games. Approved cancellation-on-restart,
maintenance/drain/backups and ADR-0010 stop-first manual replacement remain.
No high availability or automatic continuity is claimed. All cooperating runtime
processes must use the guard; old unguarded binaries cannot be made safe by it.

Tests must cover competing cohorts, multiple sessions, pool return/close,
connection termination/reconnect, and rejection of a second executable before
migrations/recovery. Actual managed-pooler and public replacement validation must
be recorded separately from local tests and CI.


## Executed evidence

CI 41883769953 and integration 42025226658 proved second-JAR rejection before
migrations/recovery. Four actual managed-session tests passed. Render B held one
cohort and rejected a competing UUID (55000), then released every guard on verified
shutdown. Render C expanded to five physical sessions in one cohort while its
complete four-player game passed. See DEV-069/070 for exact SHAs, times and limits.
This does not promise physical JVM exclusivity or uninterrupted provider updates.

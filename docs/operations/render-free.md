# Operate the occasional Render Free demo

Updated 2026-11-10. Budget **0 EUR**: no card, paid plan, addon, domain, disk or
registry publication. One Spring Boot application serves React; Supabase Dixit
Free provides PostgreSQL. Main/releases remain the owner's responsibility.

## Current publication

Current service: `dixit-demo-20261110-f`, `srv-dbpka6qfngtc73do3fk0`.
URL: https://dixit-demo-20261110-f.onrender.com
Deployment: `dep-dbpka7afngtc73do3gdg`.
Source: develop `f2602d988e6f87d5361989de9ef466d1103f8ad8`, integration CI
[42852721720](https://github.com/NicodAlba/Dixit-online/actions/runs/42852721720)
SUCCESS: frontend 33 suites/192 tests, backend 277, PostgreSQL 17 49, Python 14;
executable JAR, restored database and actual Docker image verified.
DEV-078–080 cover durable invitation retirement, shared social cards and removal
of decorative piles. Public desktop/mobile invitation review, confirmed acceptance
and automatic lobby-to-game navigation were observed; the game hand remains visible
and decorative nodes are absent. Full card-table interaction and expiration-message
visual checks were performed locally, not repeated as a complete public game.
All sixteen tables/Flyway/sequence matched the post-approved-cleanup baseline at
startup. The historical complete four-player game and idle wake remain historical;
the 226.2-second build/start observation of F is not an idle cold-start measurement.

A (`srv-dblmh5tg1s2s73ct21i0`), B (`srv-dbm7ai8u01pc738jomv0`),
C (`srv-dbm7itgae00c738ja350`), D (`srv-dbpf67rm8hqs73docftg`) E (`srv-dbpfilugekts739qqdvg`) and the isolated probe (`srv-dbl262d9fdbs73cljur0`)
remain **suspended**. Never resume a predecessor while the current service uses
this database. Do not use Manual Deploy, Restart, Rollback, scaling or an
environment save-and-deploy over a live game process.

## Configuration and accepted limits

Render Free Frankfurt: 0.1 CPU, 512 MB RAM, ephemeral filesystem. Supabase Free
Stockholm: PostgreSQL 17.11, dedicated dixit schema, separate restricted runtime
and migration roles. JDBC uses the session pooler on 5432, IPv4, verify-full TLS
and the official CA file `/etc/secrets/supabase-ca.crt`. No transaction pooler,
Supabase Auth/Realtime/Storage, browser database access or exposed Data API.

Auto-Deploy **Off**, repository NicodAlba/Dixit-online, branch develop, root Docker
context, `./Dockerfile`, default Java entrypoint, external Render PORT (10000),
health `/actuator/health`. No extra frontend, Render database, registry or disk.

Provide production.md variables privately through Render Environment. Never put
values in Git, React, Docker build arguments, logs or screenshots. Keep the JWT
key unchanged across replacements. Set the exact new HTTPS allowed origin and
replace local CA paths inside both JDBC URLs as well as SSL properties.

```text
SPRING_PROFILES_ACTIVE=prod
DIXIT_SINGLE_INSTANCE_GUARD=true
DIXIT_RECOVER_INTERRUPTED_GAMES=true
DIXIT_MAINTENANCE_ENABLED=false
```

Readiness/admissions stay closed until startup recovery completes. Interrupted
started games are cancelled without wins/rewards; accounts, completed history,
relationships, turns, votes and waiting lobbies remain. Timers, broker and presence
are not resumed. Ordinary starts reopen; explicit maintenance=true starts closed.

The guard fences **runtime physical database sessions**, including reconnections;
minimum idle one, maximum five, no extra dedicated connection. It does not prevent
Render physically creating a second JVM. A provider maintenance replacement can
fail while the old cohort is alive and need manual stop/retry; do not promise
availability or automatic continuity. See ADR-0011.

Shared Free quotas documented by Render: 750 instance hours, 5 GB bandwidth and
500 pipeline minutes/month. Idle spin-down after 15 minutes, cold starts, provider
restarts and quota suspension are accepted. No uptime pings or paid fallback.
Measured initial Spring starts took about three minutes; an observed idle wake
on A took 220.7 seconds and final C took 220.9 seconds. Do not promise a one-minute cold start. Official limits
checked again 2026-11-05 together with account quotas/no-card/zero-cost panel: https://render.com/docs/free and
https://render.com/docs/compute-plans. Current selection is Free, not a paid trial.

Render can also suspend a Free service for unusually high service-initiated public
traffic, including external JDBC. Its documented remedy is a paid upgrade, which
is outside this project's budget: stop and discuss a separately approved free
alternative instead of spending. No-card quota overages suspend/disable services
or builds instead of introducing charges. This is an accepted occasional-demo
limit, not a continuous-production guarantee.

## Start and first publication

1. Select an exact develop SHA with successful CI, including actual image/JAR,
   PostgreSQL, guard overlap rejection and isolated restore. Check remaining
   shared quotas and all predecessors suspended with matching shutdown logs.
2. For a new runtime/configuration, first create one **closed verification** Free
   service with maintenance=true, guard=true, Auto-Deploy Off and limited secrets.
   Review health, migration history, TLS/origin, React/deep links, old account/JWT
   and completed history, and actual cohort exclusion without touching game rows.
3. Suspend that verification service; confirm Tomcat/broker/Hikari closure and
   released advisory locks. Create the reviewed final Free service with its exact
   new origin and maintenance=false. This is deliberate reopening after review,
   not a second concurrently playable application. Verify public behavior before
   sharing its URL. Keep every predecessor suspended.

Render **cannot deploy a new SHA while suspended** (actual 409). Resume starts
its previous deployment. A verified B test also showed that **Save only**, then
suspend/resume, retains previous runtime environment values: maintenance remained
true. Do not use that path to apply a new version or new startup configuration.
[Official save semantics](https://render.com/docs/configure-environment-variables).
Detailed provider evidence is in DEV-062/065/070 and ADR-0010.

## Update or rollback

1. If idle, wake the current URL and wait for health UP before using the operator
   helper (its requests time out before a several-minute cold start). Owner
   authenticates through the existing interactive helper, never chat or
   password arguments: `maintenance.py --url CURRENT_URL --enable`.
2. Require enabled=true and activeGameCount=0. Let active games finish; do not
   silently cancel historical records or remove waiting lobbies.
3. Make an encrypted dump and verify isolated restoration/application startup
   using supabase.md. Keep dump, encryption key and JWT separately outside Git.
4. Suspend the current service. Require provider Suspended, no pending deployment
   and matching Tomcat/broker/Hikari shutdown logs. A failed URL or a blank metrics
   graph is not proof. Check advisory cohort locks are released before replacement.
5. For new startup/guard/security/database infrastructure, follow the closed-review/
   final-Free sequence above. For a presentation/OpenAPI patch with unchanged
   startup, admission, instance guard and migrations, the existing runtime review
   plus CI image/restore and local UI verification can precede a single replacement
   Free service after the old process has stopped. Verify the exact CI-green SHA,
   one playable service, existing normal data and the affected public UI/API.
   Distribute the **new URL** only after acceptance; never overlap playable processes.

Rollback uses the same stop-first procedure and a schema-compatible **guarded**
SHA (712116a or later verified guard implementation), not resuming unguarded A.
Code rollback does not reverse migrations/data. An incompatible data recovery
requires a separately reviewed switch after restoring into an isolated compatible
database and checking tables, sequence, Flyway and real startup. Never test restore
over the managed source. No automatic CD is configured; first and subsequent
publications are manual. Any future CD needs manual approval before publishing.

## Verification and diagnostics

Health may include liveness/readiness group names, but no components/details.
Check HTTPS/redirect, root/deep links/assets/licence notices, anonymous private API
401, authenticated REST, actual WSS/STOMP/SockJS, privacy, reconnection, a complete
four-player game, replenishment/history/statistics/achievements, idle wake and
stable data after replacement. Separate automated HTTP/WS from actual visual
observations; inherited Phase 4 visual debt/mobile/other-network checks remain.

```sh
node scripts/operations/public_acceptance.mjs --url https://APPROVED.onrender.com --private-output /private/demo-acceptance.json --acknowledge-fixtures --visual-pauses
python3 scripts/operations/public_persistence.py --url https://APPROVED.onrender.com --fixture /private/demo-acceptance.json --save-baseline /private/demo-baseline.json
python3 scripts/operations/public_persistence.py --url https://REPLACEMENT.onrender.com --fixture /private/demo-acceptance.json --compare /private/demo-baseline.json
```

Use supported Node 22 with the locked frontend ws dependency installed by npm ci.
The --visual-pauses option requires an interactive terminal before any fixture
creation; ordinary non-paused automation does not.
Synthetic PLAYER credentials/results remain private 0600 files outside Git; never
run these against an arbitrary host or ordinary CI. Keep playedCards order in
continuity fingerprints; only inherently unordered collections are normalized.
The owner-operated maintenance launchers target the current E URL. Owner account
identity is in a local protected reference, not its password. Reset uses the
backup-gated console-only helper documented in supabase.md.

For failure: inspect Render Events/logs and appropriate health only; never enable
SQL bind logging or show environment values. If a guard conflict appears, do not
disable it: establish predecessor closure, then retry through the approved path.
Free platform maintenance can require operator intervention before Render's
failure deadline. No high availability is claimed.

## Resources, backups and permissions

All 36 distinct narrative cards and public visuals now have original-art CC0
provenance; Bree Serif is self-hosted under OFL with required notices. IDs and
historical relations are preserved. See public-assets.md and /static/notices.
The six fictitious ADMINs first became PLAYER; the subsequent authorized cleanup
retired 144 exactly inventoried synthetic accounts and their test-only graphs.
The four ordinary accounts, owner ADMIN and three normal games remain. Do not
run managed acceptance again to repopulate public rankings; use isolated fixtures.
Backup verification can generate an eight-round fixture only in its disposable
restored PostgreSQL after all original rows have matched. See DEV-074.

Backups are **manual**, not supplied as an assumed Free-plan guarantee. The owner
is responsible for a verified encrypted copy before updates/migrations and on
days with real demo activity, and separate protected key/JWT custody. Existing
copies are local and outside Git; external physical custody is not accredited.
Do not upload dumps to public Actions artifacts or add paid storage. Restore
procedure and actual evidence are in supabase.md and DEV-070/071.

All evolution follows **feature/* or fix/* → PR → develop**, preserving commits.
No main changes, releases or Phase 6/7 work are part of this operation.

Operator note: avoid full accessibility/DOM dumps of Render environment forms: masked fields may still serialize secret values. Use metadata-only checks. DEV-080 records a private tool-output exposure; review rotation before sharing the technical conversation. No values belong in this manual.

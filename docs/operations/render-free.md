# Occasional demo on Render Free

Updated 2026-11-03. The owner authorizes manual publication from a CI-green develop
SHA, strictly 0 €, Supabase Dixit Free only. No main/release, card, upgrade, paid
disk/domain or Auto-Deploy. ADR-0010 records the approved changing-URL procedure.
Actual publication and acceptance evidence is recorded in CHANGELOG_DEV; a probe
URL is not a playable demo.

## Free account and configuration

Dashboard: Hobby, no card, $0 accrued/projected, shared monthly quotas of 750
instance hours, 5 GB bandwidth and 500 pipeline minutes. Free web compute is
0.1 CPU/512 MB, ephemeral filesystem, no scaling. Frankfurt app → Stockholm DB.
Idle spin-down after 15 minutes, cold starts, provider restarts and quota
suspension are accepted for this occasional demo; no uptime guarantee or paid
fallback. [Free limits](https://render.com/docs/free),
[compute plans](https://render.com/docs/compute-plans), checked 2026-11-03.

One active Docker web service serves React and Spring. Repository
NicodAlba/Dixit-online, branch develop, explicit CI-green SHA. Root/build context
repository root, Dockerfile `./Dockerfile`, default Java entrypoint, external PORT,
Auto-Deploy **Off**, health `/actuator/health`. No Render database, disk, registry
credentials, extra frontend service or build-time production secrets.

Copy the private backend variables in production.md through Render Environment,
never Git, frontend, logs or screenshots. Limited runtime and migration roles
remain separate. Stable JWT is copied unchanged. Mount the official public CA
as secret file `supabase-ca.crt`; both JDBC URLs/properties use
`/etc/secrets/supabase-ca.crt` and verify-full. Replace any owner's local CA path
inside URL query parameters too. Session pooler port 5432 provides IPv4; do not
switch to transaction port 6543 or buy IPv4.

Use the new exact HTTPS origin for DIXIT_ALLOWED_ORIGINS. Keep trusted forwarding
restricted until verified; do not invent a broad proxy regex. React uses relative
REST URLs and the current HTTPS origin for SockJS. Verify TLS, HTTP redirect and
actual authenticated transport after publication.

```
SPRING_PROFILES_ACTIVE=prod
DIXIT_RECOVER_INTERRUPTED_GAMES=true
DIXIT_MAINTENANCE_ENABLED=false
```

Recovery closes admission/health until its cancellation transaction completes.
Interrupted active games close without new wins/rewards; completed games,
accounts, relationships, cards, turns, votes and lobbies remain. It does not
recover live timers/broker/presence. Explicit maintenance=true is a deliberately
closed start requiring ADMIN reopening; ordinary cold starts use false.

## Accredited provider behavior and failed update path

The isolated Free probe `dixit-online-demo` (`srv-dbl262d9fdbs73cljur0`) deployed
commit `14eca3c1595d389ed11293e865b2601300839512`, without DB, secrets or Dixit assets.
Health and logs identified boot `0b515602-9a78-4c33-a37b-592c40529558`, instance
suffix l9hnh, PID 1. Suspension showed **Suspended by you** and `PROBE_STOP` for
that same boot at **2026-11-03T18:13:17.861Z**. The metrics chart had no series yet;
no numerical instance count is claimed from it. Free has no multi-instance scaling.

An actual POST to its existing secret hook, selecting that SHA while suspended,
returned **HTTP 409**, `{"conflict":"service is suspended"}`. Manual Deploy is
absent while suspended. Saving a Docker-command setting does not build a new
commit. [Official hook responses](https://render.com/docs/deploy-hooks) agree.
Resume started boot `3c442349-9fb8-406c-8ab8-526ee021d977`, suffix p7cc6, at
18:16:58.347Z, after the previous STOP, using the same artifact. It was suspended
again and logged STOP at **18:17:37.659Z**. This proves stop/resume, not a new-SHA
update while suspended. Private hook stays outside Git/Actions.

Render [deploy/restart behavior](https://render.com/docs/deploys) overlaps old/new
instances. Do not use Manual Deploy, Restart or Rollback on a running Dixit
process connected to this database. Memory-only maintenance does not coordinate
processes. Do not resume a suspended predecessor after a replacement is active.

## Approved stop-first publication/update (changing URL)

The owner explicitly approved this alternative after the 409 test:

1. Select a develop SHA with successful CI including actual image and restoration.
   Confirm available shared quotas, no paid selection and Auto-Deploy Off.
2. For an existing game service, owner runs `maintenance.py --enable` with hidden
   ADMIN input. Wait activeGameCount=0; let games finish, preserve waiting lobbies.
3. Create the protected backup and verify isolated restoration. Keep dump/key/JWT
   separately outside Git. No managed database is overwritten for testing.
4. Suspend the old service. Confirm provider Suspended, no pending deployment and
   matching shutdown/process-closure logs. A failed HTTP request alone is not
   proof. If closure cannot be established, stop and obtain provider evidence.
5. Create one **new Free** Docker service, with the approved SHA and the above
   external backend configuration. Never start it before predecessor closure.
   Verify actual deployed SHA and one playable service. Keep all predecessors
   suspended; no horizontal scaling or rolling update.
6. Wait health UP, then check React/deep links/assets/notices, login/registration,
   authenticated REST/STOMP/SockJS, reconnection, privacy, history/statistics.
   Publish the **new URL** only after acceptance. Existing JWT/account/history
   remain usable on the new origin; a browser may need to sign in there.
7. Exercise an idle cold start without uptime pings, and a controlled game-service
   replacement. Record HTTP, automated and visual results separately.

A new service means a new onrender.com URL. That tradeoff and downtime are
accepted, not a promise of a fixed public hostname. Suspended services are not
billed; do not delete predecessors or add resources without the relevant scope.

Rollback follows the same drain/backup/suspend/closure/new-Free-service sequence,
built from a schema-compatible previous SHA. Reverting application code does not
undo migrations or data. For incompatible recovery restore into an isolated
compatible database, contrast rows/sequence/Flyway and boot there before agreeing
any controlled switch. Never test restore over the managed source.

## Resource and administrator gates

Owner-approved original art and the self-hosted OFL Bree Serif font replace the
unaccredited inherited graphics/font. All 36 cards are distinct narrative images;
IDs and database history remain. See public-assets.md and shipped notices under
/static/notices. Code/academic notices remain; no inference from the POM's licence
was used for inherited illustrations.

Exactly six privately inventoried fictitious ADMINs were downgraded to PLAYER
only after verified encrypted backup. Passwords and history were preserved. The
owner then ran hidden terminal bootstrap and chose their own credentials. A
read-only check found one ADMIN and no synthetic ADMIN. No default password,
public bootstrap endpoint or bulk deletion. See DEV-064 and supabase.md.

## Reproducible public acceptance

After `npm ci` with the project's supported Node 22, the owner-run script creates
four identifiable PLAYER fixtures and plays eight rounds through HTTPS REST and
live authenticated WebSockets/STOMP. It checks hands/votes/order, replenishment,
reconnection, history/statistics/achievements. It never reads backend secrets or
runs in ordinary CI. Use a private owner-controlled output directory:

```sh
node scripts/operations/public_acceptance.mjs --url https://APPROVED.onrender.com --private-output /private/demo-acceptance.json --acknowledge-fixtures
```

The output contains only synthetic account credentials/tokens and must stay 0600
outside Git/public artifacts. This is automated acceptance, not desktop/mobile
visual acceptance; use optional `--visual-pauses` to stop at key game phases
while observing the actual browser product. Preserve the
Phase 4 visual debt until the relevant journeys are actually observed.

Route: feature/* or fix/* → PR → develop; main remains the owner's responsibility.


## Current supervised acceptance checkpoint (2026-11-04)

Initial service `dixit-demo-20261103-a`, id `srv-dblmh5tg1s2s73ct21i0`,
https://dixit-demo-20261103-a.onrender.com, deploy
`dep-dblmh6dg1s2s73ct23b0`, actual SHA
`8377bad18af4226c82e36c104b7cef19b3b2d828` (Actions 41874465899 SUCCESS).
Render marked Live at 18:23:13 CET. Startup on 0.1 CPU took 180.6 seconds for
Spring; do not promise a one-minute cold start. Health may include group names
(liveness/readiness), with no component/details exposure.

Public four-PLAYER acceptance completed eight rounds, reconnection, scoped REST
views, live WSS/STOMP, replenishment, friendship/invitation, history/statistics and
achievements. One participant's actual desktop UI was observed through the key
phases, including original art, social presence, chat and final reveal; this is
not four independent GUI clients or full inherited Phase 4 visual acceptance.
Mobile viewport override did not take effect, so no mobile claim is made.

`public_persistence.py` is read-only and uses only the protected synthetic fixture:

```sh
python3 scripts/operations/public_persistence.py --url https://APPROVED.onrender.com --fixture /private/demo-acceptance.json --save-baseline /private/demo-baseline.json
python3 scripts/operations/public_persistence.py --url https://REPLACEMENT.onrender.com --fixture /private/demo-acceptance.json --compare /private/demo-baseline.json
```

It checks stable JWT/private hands, all historical authors/votes, final hands,
stats/history and per-actor view fingerprints. It preserves playedCards order in
the fingerprint so an anonymous-order change after restart is detected. The
second URL must be explicitly selected as the authorized replacement; credentials
are neither printed nor sent to an arbitrary host. Ordinary CI never runs this.
Idle startup was subsequently verified: Render stopped instance 6v54q at 17:54:44Z; wake request at 17:56:25Z reached health UP after 220.7 seconds on qv4rw. Recovery cancelled zero games. The read-only continuity checker passed with all four previous JWTs and identical history/view fingerprints. Stop-first replacement to a different application SHA remains pending.

## Provider maintenance and the approved database guard (2026-11-04)

Render may overlap stateless processes during its own maintenance, including
Free without advance notice. ADR-0011 adds DIXIT_SINGLE_INSTANCE_GUARD=true to
**the next** service's required configuration. Initial service A is unguarded;
stop it before starting a guarded replacement. Do not enable this by a rolling
update over A. The guard fences runtime database sessions; it cannot prevent
Render physically starting another JVM or guarantee uninterrupted service.
A rejected provider replacement can require a manual stop/retry. Keep monitoring
Render notices. Four actual Supabase session-pooler tests passed (cohorts, concurrent claim,
connection termination/reconnect and Hikari physical-session lifetime), with no
application-table writes. Guarded public replacement remains pending; do not describe the protection as already deployed.


## Resume after the owner's computer shutdown

The public service runs on Render, independently of the owner's computer. Leave
A running; do not resume the suspended probe or create a second playable service.
The owner has already approved the guard; no new architecture permission is needed.

1. Check PR #62 CI, including actual concurrent-JAR rejection. Resolve failures,
   review the final diff, then integrate feature/single-instance-guard → PR → develop
   preserving commits. Confirm integration CI and select its exact SHA.
2. Ask the owner to run the protected `close-demo-for-update` launcher locally to
   authenticate ADMIN with hidden password; require maintenance enabled and zero
   active games. Never request the password in chat.
3. Make and verify an encrypted backup/isolated restore. Existing latest pre-demo
   backup is 20261103T182607Z; it does not include the new public acceptance game.
4. Suspend A; verify provider suspension and matching application shutdown logs.
   Keep it suspended. Create the approved replacement Free service with Auto-Deploy
   Off, exact checked develop SHA, same stable JWT/database roles/TLS CA, new exact
   origin and DIXIT_SINGLE_INSTANCE_GUARD=true. No card or paid resources.
5. Verify health, old fixture continuity against the private baseline, another full
   four-player WSS/STOMP game and actual browser UI. Record the new URL/SHA, actual
   guard evidence and controlled replacement. Only then assess Phase 5 closure.

Private operator files/configs/fixtures/backups remain outside Git or ignored;
paths and safe launcher usage are documented in the changelog/runbooks, never
actual values. Full Phase 4 visual debt/mobile/other-network acceptance remains.

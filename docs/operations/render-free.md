# First occasional demo on Render Free

Updated 2026-11-03. The owner authorizes manual publication from a verified develop
SHA, budget 0 €, Supabase Dixit Free only. No main/release, card, upgrade, purchased
domain or automatic deployment. **No Dixit service or public app URL has been
created in this intervention.** Public exposure remains blocked as described below.

## Account and intended configuration

The actual Render dashboard shows Hobby, no card on file, $0 accrued/projected,
750 instance hours, 5 GB bandwidth and 500 pipeline minutes per workspace/month.
These quotas are shared with existing services; do not alter them. Free web
compute is 0.1 CPU/512 MB; filesystem is ephemeral. Idle spin-down, cold starts,
unannounced restarts and quota suspensions are accepted for an occasional demo.
See [Free limits](https://render.com/docs/free) and
[compute plans](https://render.com/docs/compute-plans), checked 2026-11-03.

Use one Docker web service, nearest available European region to Stockholm,
repository NicodAlba/Dixit-online, branch develop, explicit CI-green commit.
Auto-Deploy **Off**, health check `/actuator/health`, default Java Docker entrypoint
and externally supplied PORT. No Render database or disk. Build from that SHA,
not a moving latest branch, and verify the deployed SHA in the dashboard.

Load the production variables in production.md outside Git. Mount the official
Supabase CA as `/etc/secrets/supabase-ca.crt`; point both JDBC configurations at
that path with verify-full. Separate limited runtime/migration credentials and
stable JWT remain backend-only. DIXIT_DB_ROLE is dixit_runtime even when the
connection username includes the Supabase project suffix. Set the exact new HTTPS
origin after allocation and verify the proxy's trusted forwarding addresses;
do not guess a broad trusted-proxy regex or disable TLS to get a green check.

For normal demo/cold starts:

```
SPRING_PROFILES_ACTIVE=prod
DIXIT_RECOVER_INTERRUPTED_GAMES=true
DIXIT_MAINTENANCE_ENABLED=false
```

Recovery closes admission and keeps health OUT_OF_SERVICE until its transaction
finishes. Active interrupted games are cancelled using the existing rule; completed
games/lobbies/history remain. The app then opens without ADMIN intervention.
Explicit maintenance=true deliberately stays closed and is for supervised
exceptions only. H2/console/seeds/create-drop remain disabled in production.

## Mandatory update sequence — not yet accredited on a Dixit service

1. Confirm the exact develop SHA has successful CI including image and restore;
   keep auto-deploy Off. Check free quotas and schema/app rollback compatibility.
2. ADMIN `maintenance.py --enable`; wait activeGameCount=0. Do not kill live games
   as planned maintenance. Waiting lobbies may remain.
3. Use the existing protected backup procedure. Keep encryption/JWT separately.
4. Suspend the specific Dixit service. Wait for actual stopped status **and zero
   running instances**, no pending deploy/restart. A button click, HTTP failure,
   202 response or health failure alone does not prove the old process stopped.
5. Only after that proof, select/build the intended commit and start one instance.
   Determine in the actual Free service whether deploying while suspended and
   resuming supports this sequence; do not assume it from endpoint names.
6. Require health UP after recovery, verify one running instance, deployed SHA,
   TLS, routes, authenticated REST/STOMP/SockJS and ordinary admission. If explicitly
   started closed, verify then ADMIN reopen; otherwise health controls reopening.

Render documents [rolling deploys/restarts](https://render.com/docs/deploys),
[Suspend](https://api-docs.render.com/reference/suspend-service-1),
[Resume](https://api-docs.render.com/reference/resume-service-1) and
[List instances](https://api-docs.render.com/reference/list-instances). The endpoints
exist; this does not prove the Free account's end-to-end stop/update/start behavior.
Do not use Manual Deploy/Restart/Rollback on a running Dixit process. If the above
sequence cannot be established, keep the app unpublished; no paid workaround.

Rollback uses the same drain/backup/stop/zero-instance sequence and an artifact
compatible with the current schema. Reverting code does not undo data changes.
For incompatible schema recovery, restore the protected dump to an isolated
compatible database, compare rows/sequence/Flyway and boot there before any
controlled switch. Never overwrite Supabase just to test restoration.

## Public resource and administrator gates

Focused inspection found:

- pom.xml declares Apache 2.0 and README preserves the Spring Petclinic/academic
  origin. This does not establish rights to bundled art or fonts.
- `frontend/src/static/fonts/American Typewriter Bold.ttf` embeds an E+F Designstudios
  copyright/ITC trademark notice and OS/2 fsType value 6. No web redistribution
  licence for that file was found in the repository. Font metadata alone is not
  permission to serve it publicly or a legal conclusion about permitted uses.
- `frontend/public/Cards`, achievements, backgrounds/icons and Dixit branding have
  no resource-specific authorization/attribution manifest in the reviewed sources.
  No such permission can be inferred from a public Git repository or the POM.

Before exposure, the owner must supply the relevant permissions/provenance or
approve replacing affected resources with clearly redistributable equivalents.
No art/font replacement or redesign is silently performed. This publication gate
is unresolved; do not upload the current app to a publicly reachable service.

The private backend config contains database/JWT credentials, not a chosen owner
ADMIN password. No owner ADMIN is invented. Existing acceptance accounts include
fictitious ADMINs; operations/supabase.md requires authorized retirement before
first-admin bootstrap. Their prefixes alone are not authority for bulk deletion.
Keep them and give the owner the protected interactive bootstrap procedure in
demo-maintenance.md; never send passwords in chat/arguments/captures.

## After these gates

First publication is manual and supervised. Reuse existing scenarios on the actual
URL: React/deep links, signup/login, four-player full game in UI, actor/phase
privacy, chat/social/notifications, authenticated STOMP/SockJS, reconnect/interruption,
history/statistics/achievements, idle cold start and maintenance/reopening.
Record actual HTTP, automated and desktop/mobile visual evidence separately.
The Phase 4 visual acceptance is still pending. No public validation or CD is
claimed; manual deployment is sufficient until this full procedure is accredited.

Route: feature/* or fix/* → PR → develop; main remains the owner's responsibility.

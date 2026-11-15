# Proposal for the first hosted demo

Historical proposal, updated November 3, 2026. The owner approved Render Free, Supabase Free and manual publication from a CI-green `develop` commit, with a strict €0 budget. The deployment was still pending at this point. See the [Render runbook](render-free.md) for the implemented procedure and the [README](../../README.md) for the demo.

## Proposed architecture

One Java 21 / Spring Boot 3.5.5 process serves React and handles REST, JWT/BCrypt, JPA and STOMP/SockJS. Supabase Free/Nano in Stockholm supplies PostgreSQL 17.11. The proposal used a single playable instance on Render Free in the nearest available European region, with Docker and HTTPS. The database had already been created and tested. See [Supabase](supabase.md), [Docker](docker.md) and [CI](ci.md).

At the time of the comparison, Supabase Free offered 500 MB of database storage, shared CPU, 500 MB RAM and 5 GB egress, with a pause after a week of inactivity. The Nano reference limits were 60 direct connections and 200 pooler clients, including provider usage. The proposed Hikari pool was 0–5, with a separate Flyway connection. Maintain encrypted backups outside the provider. [Supabase pricing](https://supabase.com/pricing) · [Compute](https://supabase.com/docs/guides/platform/compute-and-disk) · [Connections](https://supabase.com/docs/guides/database/connecting-to-postgres)

Render Free was evaluated at 0.1 CPU, 512 MB RAM and 750 hours per workspace per month, with idle sleep after 15 minutes, ephemeral storage and limited rollback. Its planned use was an occasional demo. Check quotas, latency, HTTPS, WebSockets and SockJS through the real proxy. Keep Auto-Deploy off and avoid paid storage, domains, IPv4 add-ons and upgrades. [Render free plans](https://render.com/docs/free) · [Compute plans](https://render.com/docs/compute-plans) · [Deployments](https://render.com/docs/deploys) · [WebSockets](https://render.com/docs/websocket)

## Planned release procedure

1. Select an exact `develop` SHA with successful CI, image and restore checks. Supply the [production settings](production.md) privately. Verify TLS, the CA, the trusted proxy and the exact HTTPS origin. Keep the Data API disabled and roles restricted.
2. Close admissions, wait for zero active games, verify a backup and stop the old process before starting its replacement. A default rolling deployment can overlap game engines. The accepted [maintenance contract](demo-maintenance.md) and ADR-0009 describe interrupted-game recovery and the deliberate closed-start option.
3. Verify routes and assets, registration and login, friends and invitations, a four-player match, privacy, replenishment, reconnects, cancellation, statistics, achievements and persistence. Check another device or network if possible. Verify asset rights and attribution before sharing the demo.
4. For rollback, use a schema-compatible artifact. Prefer additive migrations. If compatibility breaks, fix forward or verify a restore in a separate database before redirecting traffic.

At the proposal stage, asset clearance, owner administrator setup, fixture cleanup, replacement evidence and public and visual acceptance remained pending. The [Render runbook](render-free.md) records the later operational decisions and evidence.

# ADR-0010: Replace the free service after stopping its predecessor

Date: 2026-11-03. Status: Accepted by the owner.

## Context

Dixit uses one Spring application serving React, an in-memory STOMP broker and
presence, with Supabase PostgreSQL. Recovery may cancel interrupted games only
when no previous application process is active. Budget is strictly 0 €.
Render's default rolling deployment temporarily overlaps old and new instances.
The controlled asset/data-free probe proved graceful suspension and a later
same-artifact resume, but a new-commit deploy while suspended returned HTTP 409
`service is suspended`. The dashboard hides Manual Deploy in that state.

## Decision

The owner explicitly accepts a changing public URL between updates. Keep
Auto-Deploy Off. For the first publication and every controlled update: drain
active games through ADMIN maintenance, take the protected verified backup,
suspend the previous service and confirm process closure, then create one new
Free service at a CI-green develop SHA. Only after confirming closure may the
replacement receive backend configuration and start recovery. Publish its new
URL after acceptance. Leave the predecessor suspended; never resume it against
the same database while another service is active.

There remains exactly one playable application instance. No new broker,
authentication system, frontend hosting layer, paid disk or card is introduced.
Main and releases remain under the owner's control.

## Consequences and alternatives

Updates have downtime and require distributing a new URL. Shared Free quotas,
idle spin-down, cold starts and unexpected interruptions remain accepted. An
application rollback uses a new Free service built from a schema-compatible old
SHA, following the same stop-first procedure; resuming an old service is not the
normal rollback. Application rollback does not reverse database changes.

Keeping a fixed URL would require a separately approved operational design.
Default rolling deployment is rejected because memory-only maintenance is not
cross-process coordination. Paid disks and upgrades are outside budget. Do not
silently add a proxy, registry, database lease or distributed infrastructure to
avoid the provider limitation.

## Evidence

See operations/render-free.md and DEV-062/065. Probe lifecycle logs, unique boot
and instance identifiers, suspended provider state and the actual 409 were
observed. This decision does not itself establish successful public gameplay,
visual acceptance, cold startup or a game-service update; those need execution.


## Operational clarification (2026-11-05)

ADR-0011 explicitly approves a PostgreSQL session guard for provider maintenance;
this supersedes the earlier exclusion of adding a database guard without approval.
Manual updates still use stop-first/new-Free-service, never overlapping engines.

Actual B testing showed that saving environment changes with **Save only**, then
suspending/resuming, starts the previous deployment configuration (maintenance
remained true). Resume is not a way to deploy a new configuration. Use a closed
verification service, stop it after review, then create the final reviewed Free
service with ordinary maintenance=false. This preserves automatic reopening after
cold startup instead of silently requiring ADMIN after each idle. New public
origin and any runtime differences must be checked before sharing the final URL.

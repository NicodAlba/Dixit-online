# ADR-0009 — Minimal restart recovery for the occasional demo

Date: 2026-11-03. Status: accepted by the owner's Phase 5 completion instructions.

## Context and decision

The owner accepts interrupted live games after a process crash. PostgreSQL keeps
history but cannot reconstruct the simple broker, presence or host deadlines.
The demo must allow playing again without inventing wins or rewards.

Opt-in DIXIT_RECOVER_INTERRUPTED_GAMES=true closes new-game/start admission during
startup, selects started games without endDate under the existing pessimistic
write locks and calls the existing cancellation path in one transaction. Completed
games and waiting lobbies are excluded. Cards, turns, authors, votes, points,
accounts and relationships remain; no achievement/winner/refill code runs.
Repeating recovery finds no remaining interrupted games. Failure rolls back and
fails startup with admissions closed. Health is OUT_OF_SERVICE until recovery
completes. Existing English cancelled-game UI explains preserved scores/history
and absence of winners/new rewards; no reason field or migration is needed.

Ordinary Free/cold starts use DIXIT_MAINTENANCE_ENABLED=false and reopen after
recovery, without requiring the absent owner. Explicit true remains available for
an intentionally closed start. Scheduled maintenance closes admission via ADMIN,
lets games finish, backs up and stops the old process before any replacement.
Normal replacement recovery then has no active games to cancel; health gates
reopening. Do not permanently configure true for the ordinary demo.

## Preconditions and alternatives

This mechanism requires the previous playable process to be stopped. It is not
a lease, fencing token or coordination between applications. A rolling replacement
would cancel games owned by the old process and is prohibited. Render's default
zero-downtime deploy/restart is not approved for this operation. Stop/suspend,
zero-instance verification, replacement and health must be accredited first.

Full recovery, high availability, Redis, an external broker and a new cancellation
reason schema are unnecessary for the accepted occasional-demo policy. An
indefinitely closed ordinary startup is rejected. No data is deleted and no
completed game is reclassified. This supersedes ADR-0008's previously undecided
crash policy and cold-start/manual-reopen assumption, not its ADMIN controls.

See operations/render-free.md and CHANGELOG_DEV.md for actual evidence/blockers.

# ADR-0008 — Single-instance maintenance and first-administrator bootstrap

Date: 2026-10-30. Status: accepted explicitly by the owner before implementation.

## Context

The in-memory broker and presence require one playable process. Updates need to
stop admission while existing games finish. Accounts and history persist, but
process restart does not reconstruct active-game timers or WebSocket sessions.
A maintenance control requires an administrator without academic credentials.

## Decision

An ADMIN-only REST control exposes maintenance state and aggregate active/lobby
counts. A fair in-process admission gate blocks creation and initial start with
503 while enabled. Admission holds through transaction completion; enabling waits
for previously admitted transactions. Existing games and actor permissions keep
their rules. No record is cancelled, scored or rewritten by maintenance.

The replacement starts with DIXIT_MAINTENANCE_ENABLED=true. The operator drains
active games, verifies a protected backup, stops the old process fully, starts
one replacement, verifies it and reopens through REST. Never overlap playable
instances. The flag is per-process, so host cold starts also require reopening
when this environment flag is set.

A local, interactive Java CLI creates the first ADMIN using hidden confirmed
password input, the existing BCrypt policy and a database transaction/advisory
lock. It refuses an existing ADMIN or a privileged database role. There is no
public bootstrap endpoint, predetermined password or credential argument.

## Alternatives and consequences

Hiding frontend buttons would not enforce admission. Automatic cancellation or
reconstruction of active games changes recovery policy and remains unapproved.
Redis, external brokers and horizontal scaling are unnecessary for this demo.
A rolling deployment that overlaps processes cannot implement this procedure.
Unexpected host stops remain possible on Free hosting; public deployment and
crash-handling policy still need owner decisions. Test ADMIN fixtures must be
retired with authorization before owner bootstrap. This decision does not create
hosting, incur spending or authorize public publication.

See ../operations/demo-maintenance.md for commands and limitations; actual test
and Git evidence belongs only in ../CHANGELOG_DEV.md.

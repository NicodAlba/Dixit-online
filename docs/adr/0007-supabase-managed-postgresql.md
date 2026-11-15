# ADR-0007 — Supabase Free for managed PostgreSQL

Accepted on 2026-10-30. The owner created the project. This decision approved the database provider and left application hosting and publication for separate approval (ADR-0006, DEV-059).

## Decision

Use Supabase exclusively for PostgreSQL. Keep Spring and React together, with Spring Security, JWT/BCrypt, REST and STOMP/SockJS. React does not connect directly to the database. The project uses no Supabase Auth, Realtime, Storage or Edge Functions. Budget: €0, without paid upgrades or add-ons.

The Free/Nano project is in Stockholm. PostgreSQL 17.11 was confirmed before applying V1–V3. The migration role owns the dedicated `dixit` schema. The runtime role can read catalogs, use the sequence and perform application DML. It cannot write Flyway history or run DDL. Neither role needs superuser privileges or the right to create other databases or schemas.

Use the shared IPv4 session pooler on port 5432 with `verify-full` TLS and the provider CA. The development environment could not reach the direct IPv6 endpoint. The connection username includes the project reference, while the SQL role used by V3 does not. Flyway can have a separate endpoint and CA. Validate IPv6 connectivity before switching to the direct endpoint. Transaction pooling on 6543 was not verified and is blocked.

The Data API was disabled and the setting verified in the dashboard. API roles have no access to the Dixit schema or tables, and no Dixit tables are published through Realtime. This avoids broad RLS changes to JDBC transactions.

## Consequences

CI uses isolated databases and receives no Supabase secrets. Managed acceptance is an explicit operator action with synthetic data and private configuration. DEV-059 records the original failures and targeted reruns.

The Free project can pause. Use an encrypted pg_dump outside Git and verify pg_restore against isolated PostgreSQL 17, including roles, grants, row fingerprints, sequence and JAR startup. Never restore over Supabase for a test. See the [Supabase runbook](../operations/supabase.md) for responsibilities and frequency.

Delivery: feature/supabase-postgresql → PR → develop.

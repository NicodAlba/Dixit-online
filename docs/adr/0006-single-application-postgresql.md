# ADR-0006 — Single application and versioned PostgreSQL schema

Accepted on 2026-10-30 for local preparation and testing. See DEV-055/056. This decision preceded the hosting and restart-policy approvals.

## Decision

Run one Java 21 / Spring Boot 3.5.5 instance that serves the React 18 build from its JAR. Use a separate PostgreSQL 17 database. Keep JWT/BCrypt, REST actions and views, JPA, and the in-memory STOMP/SockJS broker. Supabase Auth, Realtime, Redis, microservices and horizontal scaling are outside this design.

Use Flyway OSS managed by Spring Boot and pin pgJDBC to 42.7.14 for the security fixes available at the time. Immutable migrations create the JPA-compatible schema, seed reference catalogs without accounts and grant runtime privileges. Hibernate validates the production schema. H2 and data.sql remain for local development and tests. The production package excludes data.sql and the original MySQL profile.

Validate PostgreSQL connectivity settings, separate migration/runtime credentials, a stable private JWT key and explicit origins before creating production data and authentication components. Disable create-drop, seed loading, the H2 console and verbose SQL in production.

## Alternatives and consequences

Static frontend hosting could serve the home page while Spring sleeps and add CDN caching. It would add a second release and origin to coordinate. A single application reduces operational work for the demo. Persisting H2 or merely swapping the JDBC URL would leave PostgreSQL constraints, sequences and locking unverified.

Flyway owns schema changes. The runtime role cannot run DDL, write catalogs or modify Flyway history. Startup uses separate migration credentials supplied outside the JAR. Moving migrations to a separate process can be considered if the hosting model requires it.

Persisted games do not restore sockets, presence or timers. The first design left interrupted games unchanged and required a separate policy before deployment. ADR-0009 records the later decision. Keep independently playable instances from overlapping.

The approved budget was €0. Render, Neon and Supabase were compared. Docker was proposed for Render. ADR-0007 subsequently approved Supabase Free for the database. Visual acceptance remained outstanding; HTTP and database tests do not replace it.

Delivery: feature/production-foundation → PR → develop.

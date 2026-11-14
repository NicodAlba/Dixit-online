# DIXIT — Online Multiplayer Game

An illustrated storytelling game for three or four players, developed with **Java 21, Spring Boot and React**. Create a lobby, invite friends, choose a card and a clue, and vote for the storyteller's card. Each player tells two stories before the final score is calculated.

Originally a collaborative **University of Seville, Design & Tests I (2025–2026)** project, this repository records its subsequent evolution into an occasional hosted demo. It derives from the course's Spring Petclinic-based SPA template. Academic authorship and upstream notices remain applicable; this presentation does not claim sole authorship or official affiliation with the commercial game.

[Play the last documented demo](https://dixit-demo-20261110-f.onrender.com) · [API documentation](https://dixit-demo-20261110-f.onrender.com/docs) · [Documentation](docs/README.md)

The free server may take several minutes to wake up. Availability and response times are limited. The deployment reference above is documented in DEV-080; it is not a live availability guarantee.

## Features

- Account registration and JWT authentication; host-controlled lobbies and invitations.
- Turn-based multiplayer with live updates, chat and authorized spectator views.
- Private hands and phase-aware responses; stable anonymous card ordering.
- Friends, presence, invitations, achievements, statistics and game history.
- Responsive, keyboard-accessible controls and reduced-motion alternatives.
- Card replenishment that preserves earlier turns, authorship and votes.

## Architecture

One Spring Boot application serves the React production build and the REST API. STOMP/SockJS notifications invalidate client state; React reloads an authorized REST view. PostgreSQL stores game history. Presence and the simple message broker belong to one application process.

| Area | Implementation |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.5, Spring Security, JPA/Hibernate |
| Frontend | React 18, JavaScript, React Router, Create React App |
| Communication | REST, JWT, STOMP/SockJS |
| Persistence | H2 for local development; PostgreSQL 17 and Flyway for the demo |
| Delivery | Maven wrapper, npm lockfile, multistage Docker, GitHub Actions |
| Hosting | Render Free application and Supabase Free PostgreSQL |

See [the current architecture](docs/architecture.md) and [architectural decisions](docs/adr/).

## Run locally

Use Java 21 and the Node version in `frontend/.nvmrc`. Start from the reviewed development branch:

```sh
git clone --branch develop https://github.com/NicodAlba/Dixit-online.git
cd Dixit-online
./mvnw spring-boot:run
```

In a second terminal, from the repository root:

```sh
cd frontend
npm ci --no-audit --no-fund
npm start
```

Open http://localhost:3000. Development uses disposable H2 data; restarting can reset accounts and games. Do not connect this development setup to the hosted database. See [local setup and verification](docs/local-development.md) for packaging, tests and configuration boundaries.

## Quality and delivery

GitHub Actions runs frontend/backend tests, isolated PostgreSQL acceptance, executable JAR checks, backup/restore and actual Docker-image checks. CI does **not** deploy the demo. Results belong to a specific commit; consult [CI documentation](docs/operations/ci.md) and [Actions](https://github.com/NicodAlba/Dixit-online/actions), rather than treating historical counts as a current guarantee.

Changes follow **`fix/*` or `feature/*` → PR → `develop`**. Only the owner promotes changes to `main` and authorizes releases or deployments. [CHANGELOG_DEV.md](docs/CHANGELOG_DEV.md) records evidence, decisions and limitations.

## Status and limitations

The demo uses a single application instance, manual backups and a manual replacement procedure. A server restart cancels interrupted games while preserving history; it does not resume sockets or timers. Free hosting can sleep or be suspended. Full visual acceptance across browsers and physical devices remains incomplete, and inherited frontend build warnings remain documented.

[Owner guide](docs/operations/demo-owner.md) · [Production configuration](docs/operations/production.md) · [Render operation](docs/operations/render-free.md) · [Supabase operation](docs/operations/supabase.md)

## Provenance and permissions

Academic source: [gii-is-DP1/dp1-2025-2026-li-02](https://github.com/gii-is-DP1/dp1-2025-2026-li-02). Template origin: [Spring Petclinic](https://github.com/spring-projects/spring-petclinic). Existing developer credits remain in [pom.xml](pom.xml) and the academic deliverables.

Current demo artwork and Bree Serif font have separate notices in [asset provenance](docs/operations/public-assets.md). The Maven Apache-2.0 declaration does not resolve all third-party artwork, historical resources or collaborative authorship permissions. See [publication review](docs/publication-review.md) for the precise scope and unresolved items. No blanket new license is granted by this README.

Screenshots will be added after a current, privacy-reviewed capture is approved; historical screenshots are not presented as the current interface.

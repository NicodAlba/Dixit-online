# Documentation map

For local execution, start with [local development](local-development.md). For current system boundaries and decisions, read [architecture](architecture.md). For provenance and publication readiness, read [publication review](publication-review.md).

## Operations

- [CI and executable packaging](operations/ci.md)
- [Production configuration](operations/production.md)
- [Docker](operations/docker.md)
- [Supabase, persistence and backups](operations/supabase.md)
- [Render Free deployment and replacement](operations/render-free.md)
- [Owner guide](operations/demo-owner.md)
- [Maintenance and administrator bootstrap](operations/demo-maintenance.md)
- [Public asset provenance](operations/public-assets.md)

The current deployment runbook takes precedence over earlier proposal-stage descriptions. Earlier statements that no demo exists are historical and must not be treated as current operational instructions.

## Decisions and history

[ADRs](adr/) explain approved architectural choices. [CHANGELOG_DEV.md](CHANGELOG_DEV.md) is the only chronological development record, including unsuccessful checks and limitations. A historical passing test count is not a current test execution.

## Academic baseline

- [D1 — requirements](deliverables/D1/Analysis%20of%20system%20requirements%20document.md)
- [D2 — design](deliverables/D2/System%20design.md)
- [D3 — test plan](deliverables/D3/Test%20plan.md)

These are preserved academic deliverables, with historical diagrams, code samples and coverage. They are not an up-to-date API specification or proof of current coverage. Approved refinements are explained in the ADRs, particularly card occurrences, private game views and cancellation. Current application-level architecture is documented separately; historical evidence is not rewritten.

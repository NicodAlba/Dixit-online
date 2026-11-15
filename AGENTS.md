# DIXIT working instructions

## Record every change

Before wrapping up a task, update only the private, Git-ignored `docs/CHANGELOG_DEV.md` as the development record (DEV-006). Keep it local and out of commits, public diffs, and pull requests.

Record the date and ID, problem and evidence, expected behavior, root cause, solution and rationale, relevant alternatives, files, impact on rules/data/API/frontend/modules, checks and actual results, risks, limitations, follow-ups, final status, and branch/commit/PR references when available. A short entry suffices for a trivial change.

The owner retired the former external architecture and SDD document after merging it into the changelog. Preserve historical entries and distinguish executed checks from inspection and hypotheses. Document approved architectural decisions in `docs/adr/` when appropriate.

## Scope and integration

- Work on one problem at a time. Preserve the architecture, model and rules unless explicitly approved.
- Use an agreed `fix/*` or `feature/*` branch off `develop` for code, documentation and configuration. Avoid direct changes to `develop` and `main`.
- Open a PR to `develop`, preserve the existing commits, and verify the push and merge. Publish only the task branch to the authorized repository.
- Include the route `fix/*` or `feature/*` → PR → `develop` in handoffs, proposals and closing reports. Name the branch and identify which steps are complete.
- The owner controls promotion to `main`. Agents should not independently commit or push to `main`, merge into it or open PRs targeting it.
- Releases and deployments require separate approval.

## Public history and local copies

- Keep `local/received-baseline` and `local/prepublication-history` private. They contain confidential historical material.
- Push reviewed branches by explicit name. Avoid pushing all branches or mirroring.
- Preserve exclusions for contracts, `.env` files, private credentials, dependencies, builds, logs and temporary files. Keep required notices and attribution. Original deliverables remain in local copies outside Git.

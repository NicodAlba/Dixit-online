# Single-application Docker runtime

Authorized scope: build and isolated CI validation only. No registry publication,
public app service, release or main changes. Supabase is the chosen managed DB;
its project validation is a separate delivery.

## Build

From the repository root on a machine with Docker:

```sh
docker build --tag dixit:local .
```

Node 22.23.4 uses npm ci and the existing lockfile. Maven wrapper selects 3.9.7,
builds Java 21 and packages the prod JAR after copying React. The final stage
contains Temurin Java 21 JRE and that JAR only, runs UID/GID 10001, and accepts
Spring arguments after its entrypoint. Base manifests are pinned by digest.
Refresh bases deliberately, with the same CI checks. No bit-identical image
digest across rebuilds is promised: base distribution packages and build tools
are fixed by their image manifests, but timestamps/network availability remain
build inputs. Test execution remains mandatory in CI before image packaging.

The context allowlist excludes .git, worktrees, contracts, docs, tests, local
configuration, existing builds and dependencies. Final exclusions cover nested
.env/key/credential files. Only public frontend source/assets enter the build.
Never pass credentials as build arguments or include populated .env files.

## Run locally after database authorization

Prepare an ignored .env.prod with owner-controlled secrets and mode 0600. It
is read by Docker, not Spring. It must contain the existing required prod
variables. No credentials belong on shell arguments or in shared commands.

```sh
docker run --rm --name dixit-local \
  --env-file .env.prod --publish 127.0.0.1:8080:8080 \
  --memory 512m --read-only --tmpfs /tmp:rw,noexec,nosuid,size=64m \
  --cap-drop ALL --security-opt no-new-privileges dixit:local
```

This command exposes only local loopback, not a public deployment. Adjust the
port mapping if PORT changes. Provider CA files must be mounted read-only and
configured in the Supabase delivery; do not disable verified TLS to make a
remote connection work. The JDBC role must not be the Supabase owner.

Runtime uses half the detected memory limit as maximum Java heap, leaving room
for native memory. The 512 MB CI smoke is not a concurrent-player benchmark or
a promise that a free application host meets the intended load.

Stop with docker stop (25 seconds). Java receives SIGTERM as PID 1 and Spring
allows up to 20 seconds for requests. Wait for termination before starting a
replacement: never run overlapping playable instances with independent brokers.
Do not stop with active games until the maintenance/outage policy is approved.
No automatic restart reconstructs presence, sockets or host timers.

## Validation and publication

See [ci.md](ci.md) for the actual image checks. Local tooling is absent on the
owner machine at preparation time; successful image evidence must come from
a real Actions run, not Dockerfile inspection. Registry publishing and the
first public service remain separate owner approvals.

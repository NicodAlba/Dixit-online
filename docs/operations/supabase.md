# Supabase PostgreSQL operations

The owner chose Supabase Free/Nano on October 30, 2026. The project is in Stockholm (eu-north-1), and SQL confirmed PostgreSQL 17.11 before migration. DEV-059 records the initial setup and validation. The Supabase production branch is separate from the Git branch named main.

## Connection and secrets

Use the IPv4 session pooler on port 5432. Obtain the hostname and username from **Connect** in the dashboard. The direct IPv6 endpoint was unreachable from the original development machine. Avoid transaction pooling on port 6543, paid IPv4 add-ons and API keys as SQL credentials.

Pooler usernames are `dixit_runtime.PROJECT_REF` and `dixit_migrator.PROJECT_REF`. The bare SQL runtime role is `dixit_runtime`. Keep credentials outside JDBC URLs. React uses the application's REST and STOMP interfaces.

Set `DIXIT_DB_SSL_MODE=verify-full` and provide the provider CA using `DIXIT_DB_SSL_ROOT_CERT`. Flyway can override the endpoint, TLS mode and CA through `DIXIT_MIGRATION_DB_URL`, `DIXIT_MIGRATION_DB_SSL_MODE` and `DIXIT_MIGRATION_DB_SSL_ROOT_CERT`. Omitted migration settings inherit runtime values. Verify hostnames and certificate chains.

Keep `.env.supabase.local` and any bootstrap configuration outside Git with permissions 0600. The scripts parse configuration as data. Spring does not automatically load that file. Supply a stable JWT key encoded from at least 64 private random bytes. Keep secrets outside chat, arguments, logs, frontend code and images.

The original acceptance setup used a Hikari maximum of five, minimum idle of zero, a 30-second checkout timeout, a 10-second pgJDBC connect timeout and keepalive. For the guarded hosted configuration, see ADR-0011. Connection recovery tests terminated selected runtime sessions and did not simulate a physical network outage.

## Roles and initialization

Use separate LOGIN roles, `dixit_migrator` and `dixit_runtime`, without SUPERUSER, CREATEDB, CREATEROLE, REPLICATION, BYPASSRLS or inherited privileges. The original connection limits were two and eight respectively. The application pool caps at five. The migrator owns the `dixit` schema, while runtime has no CREATE rights or catalog and Flyway-history write access.

For an authorized empty project, create both roles from the owner account with strong passwords entered through hidden input. Grant CONNECT and allow the migrator to create its schema, then revoke temporary database CREATE. The original setup used local SCRAM verifiers, SQL via stdin and a protected temporary pgpass file. Avoid entering cleartext passwords in the SQL Editor.

The schema has no USAGE or default grants for PUBLIC, anon, authenticated or service_role. Keep the Data API disabled and verify grants and Realtime publications. Preserve V1–V3 and use Hibernate validate. V2 seeds 36 images, 12 badges and two authorities, without creating accounts or games.

## Managed test-project acceptance

Run these commands exclusively against an explicitly approved test database. They create synthetic accounts and games, including test administrators. Use Java 21, the current React build and production JAR, and PostgreSQL 17 tools. The scripts reject CI, require a private configuration and explicit project reference, and require acknowledgement of test data.

```sh
python3 scripts/operations/supabase_check.py \
  --config /absolute/path/.env.supabase.local \
  --pg-bin /absolute/path/postgresql17/bin \
  --project-ref YOUR_PROJECT_REF --phase migrate --acknowledge-test-data
```

The `migrate` phase requires an empty schema and verifies startup, Flyway, Hibernate and reference catalogs. Run it once on the authorized empty target. The other phases use the same command with a different `--phase`:

- `permissions`: PostgreSQL version, migrations, catalogs, grants and Realtime.
- `acceptance`: REST/STOMP, privacy, concurrency, friends, invitations, a four-player/eight-turn game, rotation, replenishment, history, statistics and achievements, followed by restarts.
- `cancellation`: six targeted reconnect/cancellation cases using the production 30,000 ms grace period. Isolated CI keeps the shorter 2,000 ms fixture.
- `restart`: verify accounts, JWT, lobby and completed history across two JAR starts. Check the 16 tables and sequence, absence of reinsertion and recovery after terminating selected runtime connections.

Keep logs and reports under `target` outside Git. DEV-059 records the initial 34/35 pass count and the targeted 6/6 rerun with the production timeout. Those checks establish neither hosted capacity nor visual acceptance.

## Encrypted backup and isolated restore

The owner or operator runs this before application or schema changes and after demo sessions. Keep the seven most recent verified backups initially and retain at least one valid copy. The helper does not schedule backups.

```sh
python3 scripts/operations/supabase_restore.py \
  --config /absolute/path/.env.supabase.local \
  --pg-bin /absolute/path/postgresql17/bin \
  --project-ref YOUR_PROJECT_REF \
  --backup-dir /private/owner/backups \
  --key-file /separate/private/location/backup-passphrase.key \
  --acknowledge-test-data
```

The helper dumps the `dixit` schema with ownership and ACLs via TLS as the migrator. It encrypts the custom-format dump with GnuPG AES-256, removes the initial plaintext, decrypts the backup for verification and compares SHA-256. It restores to isolated loopback PostgreSQL 17 with distinct local credentials. It compares all rows across 16 tables, Flyway history and the sequence, then checks production JAR restarts. It cleans up its temporary cluster and never restores over Supabase.

Store the encrypted archive and verification metadata in a private directory with permissions 0700. Keep the encryption key and JWT key separately. The original private paths were `~/.local/share/dixit-backups/supabase` and `~/.config/dixit/backup-passphrase.key`. Keep a protected copy of backup and keys off the original machine. Test compatibility before using a restore with a different application version. DEV-059 records executed evidence.

## Free plan and publication

The October 30, 2026 comparison recorded 500 MB database storage, shared CPU, 500 MB RAM, 5 GB egress, two active projects and a pause after an inactive week. The Nano reference was 60 database connections and 200 pooler clients. Check current limits and actual usage. The database retains history, while the application keeps sockets, presence and deadlines in memory. See the [Render runbook](render-free.md) for publication and recovery. [Pricing](https://supabase.com/pricing) · [Connections](https://supabase.com/docs/guides/database/connecting-to-postgres) · [Compute](https://supabase.com/docs/guides/platform/compute-and-disk) · [Data API](https://supabase.com/docs/guides/api/securing-your-api)

Maintenance acceptance is available as `--phase maintenance` after the approved
implementation: ADMIN control, admission transaction completion/rollback and the
full four-player game finishing while closed (six targeted cases). It creates
fictitious fixtures; it does not run first-admin bootstrap on the managed database.

### Owner administrator acceptance (2026-11-03)

The owner explicitly authorized downgrading exactly the six privately inventoried
synthetic administrators to PLAYER. A new encrypted dump was restored and its
16 tables, Flyway history, sequence and production JAR verified first. Only the
six authority references changed; accounts, passwords and game history remain.
The owner then ran the existing interactive bootstrap and chose their own hidden
password. Read-only verification found one owner ADMIN and no synthetic ADMIN.
Do not rerun retirement as routine housekeeping or broaden its scope.

For an old completed-game acceptance fixture, `supabase_restore.py --history-fixture`
can refresh a short-lived synthetic PLAYER token solely for the isolated restored
database's smoke checks. It does not extend production token lifetimes or alter
the managed database. Backup files, encryption keys and fixtures remain private,
outside Git; the owner is responsible for separate protected custody.

## Forgotten owner administrator password

The chosen product password is never stored in documentation/configuration; only
its BCrypt hash is persisted. With explicit owner authorization, first query the
existing ADMIN identity using the restricted JDBC role, then create and verify a
protected encrypted backup. Use `scripts/operations/reset_admin_password.py`
with protected backend config, the production JAR and `--verified-backup` outside
Git. The owner personally enters the exact username/account ID and chooses a
new password twice through hidden terminal input. Only that ADMIN row's password
changes; role, ID and history remain. No endpoint, default password, shell password
argument or infrastructure-secret reuse. Existing JWTs are not globally revoked
immediately; this pre-existing limitation remains until their normal expiry.

## Authorized fixture removal (2026-11-10)

The owner authorized removing 136 security_/restart_ identities and eight demo_ identities verified against private inventories and acceptance files. Four regular accounts, the owner ADMIN and three games were retained. Use isolated PostgreSQL for ordinary testing.

`scripts/operations/retire_test_accounts.py` requires an inventory with permissions 0600, a verified encrypted restore and explicit acknowledgement. It defaults to a rollback rehearsal. `--apply` commits only the listed removals. It aborts on identity or role changes, active games or ordinary participation. Prefixes alone do not authorize removal.

After cleanup, `supabase_restore.py` verifies the original rows. Without a valid synthetic eight-turn game, it uses the existing test to create one only in the isolated restored database for restart checks. That fallback requires the project sources, Maven and Java 21.

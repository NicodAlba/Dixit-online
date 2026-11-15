# Demo owner's guide

## Open the demo

Use the current demo link in the [README](../../README.md), then choose **Create account** or **Log in**. The free server can take several minutes to wake after inactivity.

Your administrator identity is stored locally in `~/.local/share/dixit-review/owner-admin-reference.txt`. Keep your password private. For recovery, follow the [Supabase runbook](supabase.md#forgotten-owner-administrator-password) with a recent verified backup.

## Keep the service free

Use the existing free plans. Check shared quotas in Billing and the [Render free-plan documentation](https://render.com/docs/free). An inactive Supabase project can also pause. Check its dashboard if the application cannot start.

## Update or close admissions

Follow the [Render replacement procedure](render-free.md). Avoid Manual Deploy, Restart or Rollback on the live service and leave the previous services suspended.

Wake the current demo, verify the local launcher points to it, then run:

```sh
/home/nico/.local/share/dixit-tools/close-demo-for-update
```

Enter the administrator credentials in the terminal. The password input is hidden. Require `enabled: true` and `activeGameCount: 0` before replacing the service. Let any live games finish, verify the backup, and confirm the old process has stopped. For maintenance without a replacement, the `open-demo-after-update` helper reopens admissions. Update the private launchers when the service address changes.

GitHub pushes do not trigger a deployment. The owner controls releases and promotion to `main`.

## Backups and failures

Keep a verified encrypted backup before changes and on days with real activity. Store the backup key and JWT key separately from the dump and keep a protected copy off the original machine. See the [Supabase runbook](supabase.md).

After a crash, the database retains accounts and history. The documented recovery policy cancels interrupted games. It cannot recreate live connections or timers. If a replacement is refused by the database guard, verify the previous process has stopped and then retry. Share only a sanitized error message.

Visual checks on other browsers, networks and physical devices remain limited.

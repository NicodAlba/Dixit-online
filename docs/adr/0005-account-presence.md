# ADR-0005 — Account presence and game presence

Approved on 2026-10-25 and implemented in DEV-037.

A stored JWT does not indicate whether a friend is connected. Track authenticated STOMP sessions in memory after SessionConnectedEvent, using User.id from the validated principal. An account is online while any valid session remains. The application connection does not subscribe or send messages, and does not extend the host's 30-second game deadline.

Existing CONNECT, subscription and outbound checks remain. Expired or deleted accounts are removed and their transport is closed with ERROR. Recheck every five seconds and before reading presence. A transient database failure does not mean a user disconnected. Ten-second STOMP heartbeats help detect abandoned connections. Cleanup is idempotent and closing one tab leaves the account online if another valid session remains.

`GET /api/v1/presence` is available to PLAYER accounts and returns `{accountId, online}` for accepted friends. With `gameId`, it lists only participants of a game the actor is allowed to view, including an authorized spectator perspective. There is no global directory, arbitrary account lookup or access to sessions or tokens.

React refreshes authorized snapshots every five seconds and on window focus after CONNECT. It ignores stale reads, quietly resets its baseline after reconnecting and compares valid snapshots before showing notices. Unknown or failed reads do not count as offline. Polling can miss shorter outages. Authentication rejection stops retries and prompts the user to log in again.

Reuse the existing Spring scheduler and simple broker. Presence is lost when the process restarts. This does not add global JWT revocation when a password changes or establish physical network-loss behavior.

Delivery: feature/account-presence → PR → develop.

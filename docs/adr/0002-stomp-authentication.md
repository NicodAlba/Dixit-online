# ADR-0002 — Authenticate STOMP with the existing JWT

Accepted and implemented on 2026-10-23 in U4 (DEV-021).

The HTTP Authorization header did not authenticate STOMP CONNECT. The broker previously accepted subscriptions and client-sent state without the required checks. Keep the existing SockJS transport, simple broker and destinations, with REST handling game actions.

Validate the JWT signature and expiry and resolve the account on CONNECT. Bind the authenticated principal, restrict game and turn subscriptions to eligible users, and recheck token, account and access before delivering messages. Deny client SEND frames. For a deleted lobby, previously authorized subscribers may receive only its minimal deletion notice. Clear connection state on disconnect.

Remove the fixed signing-key default. `DIXIT_JWT_SECRET` accepts a private Base64 key of at least 64 bytes. Development generates an ephemeral key when none is configured. Deployments that need sessions across restarts must keep a stable private key outside Git. Treat previously published signing keys as compromised.

The React lobby sends the JWT on CONNECT and omits frame debug output. Transport tests wait for broker subscriptions and use server events. STOMP authentication alone does not filter private game state among legitimate players. See ADR-0003. Password changes do not immediately revoke every JWT.

Delivery: fix/stomp-permissions → PR → develop.

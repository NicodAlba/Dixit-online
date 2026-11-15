# ADR-0003 — Game views by actor and stable anonymous ordering

Accepted and implemented in U5 (DEV-023).

Authenticated subscriptions alone cannot keep hands, answers, authorship and votes private within a game. REST routes for games, turns and players therefore return explicit projections. A participant receives their hand, public hand sizes and their own flags. Eligible spectators can switch among approved perspectives in the same game. An ADMIN outside the game receives administrative catalogs without cards. The answer and votes are revealed at round close, with the storyteller and their authorized perspective allowed to see the storyteller's answer earlier.

STOMP keeps the existing destinations and JWT. Notifications carry only type and game/turn IDs. React fetches one authorized REST snapshot, drops late responses and reloads on reconnect without replaying actions. Events follow commit. The outbound guard refuses payloads with extra fields. An initial invalidation directed to the installed subscription closes the GET/SUBSCRIBE race.

GameViews projects a snapshot in a separate transaction after the command, under the existing game lock and without reusing previously loaded OSIV entities. The nullable private `turns.table_order_seed` orders a copy of the table using HMAC and the Card ID. This keeps order stable across views and reloads without exposing submission order or changing stored lists. Older active turns receive a seed under lock. Completed turns remain untouched. The seed is separate from the JWT key and absent from REST and STOMP.

Sending full graphs, hiding secrets with CSS, reordering on each GET and sorting by ID were rejected. Existing action routes, card identities, scoring and replenishment remain. The authorized spectator list is retained for results, while social discovery is limited to active games. Development create-drop adds the column automatically. A persistent database requires a planned migration.

Delivery: fix/game-phase-privacy → PR → develop.

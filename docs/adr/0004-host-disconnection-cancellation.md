# ADR-0004 — Host reconnection and cancellation with history intact

Accepted and implemented in DEV-024 on 2026-10-23.

Allow the host 30 seconds from the detected loss of their last game connection. Reconnecting in that interval keeps the game going. Explicit departure or expiry cancels it. Keep participants, cards, authors, images, rounds, votes and points already earned. Award no new wins or achievements, and skip replenishment. A completed game cannot become cancelled.

A non-host keeps their seat, hand and score and can return to the same seat by game ID. The game may wait for that player's next action. Existing membership is enough to regain the seat without the lobby password. New joiners still need an open lobby. Before the game starts, the earlier host-exit and lobby-removal behavior applies.

`Game.cancelled` distinguishes a cancelled game from a completed one. Both get an end date. PLAY achievements and completed-game statistics exclude cancellations. TRICK calculations disregard cards without a recorded author and retain actual authored plays.

Track installed, authorized subscriptions in memory. Multiple tabs do not trigger departure until the last relevant subscription is lost. Generation-tagged deadlines distinguish old timers from new ones. Cancellation uses the game lock and is idempotent. Publish the notification after commit. Failures roll back or retry without dropping history.

This policy depends on one process. The deadline begins at server detection and does not survive a restart. Tests can use 2 seconds through a test-only property; the default remains 30,000 ms. Network loss on another physical device was not measured here.

Delivery: fix/game-presence-cancellation → PR → develop.

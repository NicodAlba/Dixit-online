# ADR-0001 — Card occurrences and replenishment without losing history

- Date: 2026-10-22.
- Status: Explicitly accepted by the owner when approving item 7.C.
- Evidence: DEV-012 and the implementation in DEV-013, recorded in the private development log.

## Decision

An `Image` is an illustration. A `Card` is one occurrence of that illustration in a game. A played card keeps its identity, round, image, author and votes and is never dealt again. Reusing an image creates a new `Card` without inherited round, author or votes.

Deal randomly from illustrations that have never been dealt first. If those run out, fill the remaining slots from eligible illustrations in finished rounds. Exclude images already in hands, on active tables or reserved for the current refill. An illustration from the round that just ended is eligible immediately, including for the player who used it. This priority clarifies rule R7 by explicit owner decision.

`Game.deck` includes the reserve, hands and history. The schema and 36 illustrations stay the same. With four players, the game creates 36 cards initially, deals 24 and holds 12 in reserve. Three refills exhaust the reserve. Four more create 16 cards, for 52 occurrences total. At the end, 32 are historical and 20 remain in hands. With three players, the initial 36 suffice and 33 are dealt. The final round has no refill.

The last vote, scoring and the next round's six-card hands commit together. The existing game lock serializes advancement, with checks for the current state and any successor. Vote, closing and refill notifications are sent after commit using the existing payloads and destinations.

## Alternatives and consequences

Reassigning a historical card or clearing its round and votes would destroy information and leave React treating its ID as a played card. Separate physical-card and play entities would permit a fixed physical deck but require a broader migration. Mixing new and historical images immediately would violate the agreed priority.

The number of rows and the serialized game can grow without changing the contract shape. Availability depends on distinct images. New cards count for statistics or achievements once actually played with an author and round. This decision did not address the separate User/Player statistics issue, entity privacy, general authorization or reconnects.

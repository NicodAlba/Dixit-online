# Public demo assets and provenance

The owner approved replacement of resources without documented redistribution
rights. No database migration or data reset: Image IDs 0–35, Card history and all
badge URLs are unchanged. Their artwork is now the original demo edition. A
historical Image ID therefore renders the new illustration; no historical row,
author, vote or relationship is rewritten.

## Original visual edition — CC0-1.0

All 36 numbered card illustrations, card back, five scenic backgrounds, achievement
badges/lock, fairy/parchment icons, geometric wordmark/emblem, favicons and app
icons were created for this project as original vector compositions, with code
assistance. No input photograph, illustration, tracing, commercial game art or
third-party logo was used. The project dedicates these original assets to the
public domain under CC0-1.0 to the extent legally possible; full text in
`docs/licenses/Original-art-CC0.txt`.

- Editable masters and 36 scene titles: `assets/original-art/`.
- Creation source: `scripts/assets/generate_original_art.py`.
- Exact output inventory, licence and SHA256: `assets/original-art/outputs.json`.
- Replaced paths: `frontend/public/Cards/*.png`, `frontend/public/Achievements/**`,
  `frontend/public/{favicon.ico,logo192.png,logo512.png}` and every asset under
  `frontend/src/static/images/`. The inherited wordmark `.webp` is removed;
  its import now uses the original `.png`.

Scenes include a sky whale garden, a travelling snail town, a key forest, a
mountain in a teacup and a castle in an hourglass. The cards are distinct narrative
images without visible IDs, text or number placeholders. Composition, bright
colours, existing layout and gameplay are retained. The geometric DIXIT wordmark
is an original project identifier, not the publisher's logo; no official affiliation
is implied.

To regenerate intentionally, use a private/local Python environment with
CairoSVG 2.8.2 and Pillow 12.0.0, then run the generator from the repository. Normal
Maven/npm/Docker builds never regenerate assets or academic deliverables. The
SVG masters are the reproducible sources; generator output inventory hashes must
be refreshed if artwork changes. Generation tools are not runtime dependencies.

## Bree Serif — SIL OFL 1.1

American Typewriter is removed. The demo self-hosts unmodified Bree Serif Regular
by TypeTogether from the official Google Fonts repository. The exact upstream
revision is pinned in `docs/licenses/BreeSerif-source.txt`; OFL copyright/reserved
names and FONTLOG are retained in that directory. Embedding and redistribution
follow the OFL; the font is not renamed or sold. It is separate from CC0 artwork.
The external Google Fonts import is removed, so no font request goes to Google.

## Other notices

Existing code/developer/academic origin notices remain in README and pom.xml.
Dependency licences remain distinct; production React generates its dependency
LICENSE text as before. This resource replacement does not relicense upstream
Java/JavaScript code. The old binary resources remain only in Git history, not
in the current source tree, production frontend or Docker input. No history rewrite.

The full font/CC0 notices also ship in frontend/public/static/notices, reachable
anonymously under /static/notices; Home links to ASSET-NOTICES.txt. Repository-only
notices are not treated as sufficient accompaniment for the redistributed font.

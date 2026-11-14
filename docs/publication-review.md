# Publication readiness review

Reviewed 2026-11-14 against develop `69b58b49c783c4bd6c222dcc7a79f645ef95951b`. This is an evidence register for a publication decision, not a new license or legal clearance.

## Current exposure and approval boundary

GitHub metadata checked during Phase 6 reports the repository already **public**, with `main` as default and no detected repository license. The local tree has no root LICENSE/NOTICE, while pom.xml declares Apache License 2.0. Public availability is not proof of permission. This documentation branch has not been pushed; changing visibility, publishing it and rewriting existing history require the owner's decision under this task.

| Material | Evidence | Status and next action |
| --- | --- | --- |
| Student/team code | Academic origin, POM developer credits and D1/D2/D3 | Collaborative authorship confirmed; publication/licensing consent for all contributions not evidenced here. Owner must supply existing grants or obtain confirmation. |
| University/course template | README identifies the course and Spring Petclinic provenance | Preserve attribution. Pin the actual inherited template revision and its license; do not assume every course contribution has the same grant. |
| Spring Petclinic upstream | Official repository includes LICENSE.txt; local POM declares Apache-2.0 | Exact inherited revision and notice obligations still need reconciliation. Do not blanket-relicense student work. |
| Learning agreement | Prior local review found conditional withdrawal wording, not a general publication prohibition | Private evidence only; not permission for all code. Confirm other course agreements and circumstances with the owner/instructor if unclear. |
| Current original artwork | assets/original-art, generator, output manifest, CC0 notice and public-assets.md | Documented original-art dedication; confirm ownership scope and shipped inventory before broad redistribution. No commercial artwork permission inferred. |
| Bree Serif | Pinned upstream revision, OFL, FONTLOG and shipped notices | Documented font license, distinct from code and artwork. |
| Legacy fonts/artwork | Remote-reachable history contains American Typewriter and earlier card artwork | Rights unresolved; current replacement does not remove historical exposure. Review existing public history before promoting the source repository. No purge performed. |
| Other bundled fonts | Remote history also contains Montserrat, Varela Round and academic-report Glyphicons font files | Inspect their exact versions, accompanying licenses and current packaging; not cleared by the Bree Serif notice. |
| Java/npm dependencies | POM, lockfile and bundled notices | A frontend inventory is linked below; Java transitive-license and full notice reconciliation remain pending. No claim of complete dependency clearance. |
| Academic deliverables and screenshots | D1/D2/D3, reports and diagram images | Preserve authorship; review embedded third-party images and personal data before promoting them. Screenshots not added in this intervention. |
| Product identifier | DIXIT name and original replacement wordmark | Original drawing does not settle trademark rights; no official affiliation claimed. Scope of public branding remains an owner review item. |

## Institutional sources

The [University of Seville's 2025 intellectual-property regulation](https://www.us.es/bous-numeros/numero-11-21-de-julio-de-2025/acuerdo-111cg-24-6-25-por-el-que-se-aprueba-la-normativa) is relevant background. Rules specifically concerning final-degree work must not automatically be applied to this course assignment. Establish the applicable category, actual contributions and agreements before drawing a conclusion.

The [BOE record for RD 1791/2010](https://www.boe.es/buscar/act.php?id=BOE-A-2010-20147), checked during this review, is marked repealed. It must not be quoted as current legal authority without checking its replacement and applicability. No legal clearance is based on that old statute.

[Spring Petclinic upstream license](https://github.com/spring-projects/spring-petclinic/blob/main/LICENSE.txt) is a reference to investigate provenance, not proof of the precise historical template revision used here.

## Repository hygiene evidence and limits

A path inventory of objects reachable from fetched origin references found no Learning Contract paths and no files named `.env` or ending `.pem`, `.key`, `.dump` or `.gpg`. It did find the legacy font paths above. This is a path check, **not** a full content-based secret scan or proof of absence of personal data. Private local branches were deliberately excluded from that public-reference inventory and must never be pushed.

DEV-080 records a prior private-tool-output exposure of database/JWT credentials. Rotation status has not been established here. Do not copy those values or technical transcripts into documentation. Confirm remediation privately with the owner; do not rotate or expose credentials during documentation work.

## Decisions needed before publication of this branch

1. Establish the team's existing license grants/consent and the applicable course terms. Avoid requesting a new grant if an adequate existing one can be evidenced.
2. Decide how to address unverified historical artwork/fonts and any remaining bundled resources, after reviewing the exact affected paths. Options include securing permission or a separately approved history-cleaning/publication plan; no history rewrite is authorized here.
3. Complete dependency/content-secret and personal-data review, and confirm the prior credential incident's remediation status.
4. Approve the concrete documentation diff before pushing this local branch to the already-public repository.

Local preparation can continue independently. Intended delivery remains **feature/phase6-publication-readiness → PR → develop**; push, PR and merge are pending. Main, releases and hosted infrastructure remain under the owner's control.

## Additional verification in this intervention

- GitHub confirms [CI run 42916766915](https://github.com/NicodAlba/Dixit-online/actions/runs/42916766915) completed successfully for develop `69b58b4` on 2026-11-10. Rechecked on 2026-11-14; no new suite was executed by this documentation task.
- The public demo URL could not be read through the web tool. This is an access limitation, not evidence that Render is down. The deployed SHA remains the last documented one; no dashboard deployment identity was independently verified here.
- Java 21 and Maven wrapper 3.9.7 execute locally. The default shell has Node 18.12.1/npm 8.19.2, unlike the pinned Node 22.23.4. A clean install/build under the correct runtime has not been repeated in this intervention.
- A selected-pattern scan read 1,596 blobs reachable from fetched origin references, without printing values. No private-key or selected provider-token pattern matched. JWT-setting candidates occurred in historical `application.properties` and `JwtUtils.java`; DEV-021 already documents removal of the fixed signing-key default. These are candidates, not newly verified active credentials. Other token formats, personal data, releases and server-side objects outside fetched refs are not covered.
- [Frontend dependency inventory](licenses/frontend-dependency-inventory.csv) covers 1,733 lockfile entries. The lockfile itself lacks license fields. Metadata was taken only from installed package.json files with an exactly matching version: 1,726 provide a license label, four lack one, two installed versions differ and one package is unavailable. Version equality is not integrity verification; metadata is not a replacement for license texts or a compatibility decision. The inventory does not grant rights or certify all notices are shipped.

This remains a partial readiness review. Full changelog/design-document reading, source-to-diagram verification, Java dependency inventory, remaining resource provenance and end-to-end local-command verification must be completed before calling Phase 6 complete.

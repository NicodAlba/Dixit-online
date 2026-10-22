# Documentation of AI Use in the Project
**Subject:** Diseño y Pruebas (Grado en Ingeniería del Software, Universidad de Sevilla)  
**Academic year:** 2025/2026 <!-- p.ej., 2025/2026 -->  
**Group/Team:** LI - 02<!-- p.ej., L4-03 Equipo 33 -->  
**Project name:** Dixit <!-- p. ej. Petris -->  
**Repository:** [Link](https://github.com/gii-is-DP1/dp1-2025-2026-li-02) <!-- URL del repo -->  
**Members:** Nicolás de Alba León (PNS5576 / nicdeleo@alum.us.es), Alonso Gudiel Güemes (XSK0385 / alogudgue@alum.us.es), Víctor Martínez Trujillo (XCN8537 / vicmartru@alum.us.es), Pablo Romero Gómez (GFQ0792 / pabromgom@alum.us.es), Álvaro Vílchez Alejandre (NYP0448 / alvvilale@alum.us.es)<!-- Nombre Apellidos (US-Id / correo @us.es) -->

## 1. Introduction

This document describes the use made of AI in the project. The objective is to be transparent about the AI usage implemented. As a reminder to students, we include a summary of what is indicated in the course Syllabus:

### Policy Statement and Commitment

> **General Guiding Principles (Summary):** > - **Cognitive Dimension:** Working with AI must **not** reduce your ability to think clearly; use it to **facilitate**—not hinder—learning.
> - **Ethical Dimension:** The utilization of AI must be **transparent** and **aligned** with academic integrity.

**Specific Course Rules:**
- ✅ **AI for Code:** The use of generative technology is allowed to **complete or generate code examples** in tasks, but the source must be **explicitly cited**. Furthermore, the student must **understand** and be able to **modify upon request** any code submitted, remaining responsible for any behavior of the code they have committed, before the professor and their peers. Remember: **You are responsible** for said code.
- ❌ **AI for Narrative:** Unless indicated otherwise, using generative AI to **write the narrative** for deliverables is **not** allowed. It can be used as a **resource** during the process, but **not** to **answer exercises for you**.

**US Ethical Framework:** Consult and comply with the **Guidelines on Ethics and AI** of the US (University of Seville): https://guiasbus.us.es/ia/etica

**Filling out this document is Mandatory:** The **documentation of AI use** is a **deliverable** of the project.

## Summary by Sprint (1–4)
### Sprint X — AI Use Summary

Registered uses: Main scopes: Value added: Relevant risks and mitigations: Lessons learned: Compliance Checklist for Ethical AI Use in Sprint X:

- [ ] All significant interactions are in the Detailed Log with a link to the conversation.

- [ ] AI was not used for narrative (or there is documented authorization).

- [ ] Every accepted piece was understood and verified by humans (tests/review).

- [ ] Citations/Attributions included where applicable.

- [ ] AI was used without providing personal/sensitive data that could be exposed to external tools.

Repeat this subsection for Sprints 2, 3, and 4.


### Sprint 2 — AI Use Summary

Registered uses: 6

Main scopes: Security

Value added: Problems with password encoding resolved.

Relevant risks and mitigations: -

Lessons learned: use of passwordEncoder

Compliance Checklist for Ethical AI Use in Sprint X:

- [ X] All significant interactions are in the Detailed Log with a link to the conversation.

- [ X] AI was not used for narrative (or there is documented authorization).

- [ X] Every accepted piece was understood and verified by humans (tests/review).

- [ X] Citations/Attributions included where applicable.

- [ X] AI was used without providing personal/sensitive data that could be exposed to external tools.

### Sprint 3 — AI Use Summary

Registered uses: 2

Main scopes: Security

Value added: Problems with password encoding resolved.

Relevant risks and mitigations: -

Lessons learned: use of passwordEncoder

Compliance Checklist for Ethical AI Use in Sprint X:

- [ X] All significant interactions are in the Detailed Log with a link to the conversation.

- [ X] AI was not used for narrative (or there is documented authorization).

- [ X] Every accepted piece was understood and verified by humans (tests/review).

- [ X] Citations/Attributions included where applicable.

- [ X] AI was used without providing personal/sensitive data that could be exposed to external tools.

### Sprint 4 — AI Use Summary

Registered uses: 6

Main scopes: Generation of images and layout, tests, webSockets

Value added: Visually appelaing aplication.

Relevant risks and mitigations: -

Lessons learned: Creation of webSockets

Compliance Checklist for Ethical AI Use in Sprint X:

- [ X] All significant interactions are in the Detailed Log with a link to the conversation.

- [ X] AI was not used for narrative (or there is documented authorization).

- [ X] Every accepted piece was understood and verified by humans (tests/review).

- [ X] Citations/Attributions included where applicable.

- [ X] AI was used without providing personal/sensitive data that could be exposed to external tools.

## Detailed Log of AI Use by Sprint

**Use one row per “truly significant use”** (idea suggested by AI, important piece of code modified, debugging an error you couldn't solve on your own, generation of tests for production code, etc.). Do not include rows for trivial details such as variable autocompletion or method signatures, or simple code generation (looping and processing data structures, formatting and/or creating CSS styles, etc.).

### Sprint 1 Detailed Log of AI Use

|   # | Date and Time | Sprint | Member(s) | **Tool & Version** | **Access** | **Link to Conversation / Prompt** | **Purpose** | **Affected Artifacts** | **Human Verification** | **Risks & Mitigations** | **Outcome** |
|----:|---------------|:------:|-----------|--------------------|------------|-----------------------------------|-------------|------------------------|------------------------|-------------------------|-------------|
| 1.1 |               |   1    |           |                    |            |                                   |             |                        |                        |                         |             |


### Sprint 2

|   # | Date and Time    | Sprint | Member(s)            | **Tool & Version**          | **Access**    | **Link to Conversation / Prompt**                                                                                                                                                                                                            | **Purpose**                                                                                                                                                           | **Affected Artifacts**  | **Human Verification**                                                                                                              | **Risks & Mitigations** | **Outcome**                                                                                                                                                                                               |
|----:|------------------|:------:|----------------------|-----------------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2.1 | 02/11/2025 12:30 |   2    | Alonso Gudiel Güemes | GitHub Copilot (GPT-5 mini) | VSCode Plugin | Briefly, I asked for help because passwords were being saved in the database without encoding, and when trying to log in, unencoded passwords were being decoded, so they weren't working.                                                   | Assistance to encode user passwords                                                                                                                                   | UserService             | Code review and frontend testing                                                                                                    |                         | The code works and passwords behave correctly                                                                                                                                                             |
| 2.2 | 01/11/2025 20:30 |   2    | Nicolás de Alba León | Chatgpt (GPT5)              | Web           | Asked for assistance on how to implement ManyToMany in User.java and how to do the inverseJoinColumns                                                                                                                                        | Correctly implement Springboot code for the model                                                                                                                     | User                    | Code review and frontend testing                                                                                                    |                         | Works correctly and is well implemented                                                                                                                                                                   |
| 2.3 | 02/11/2025 12:30 |   2    | Nicolás de Alba León | Chatgpt (GPT5)              | Web           | Asked about CSS styles for the lobby page                                                                                                                                                                                                    | To have interactive CSS that serves as an example to visualize what we want                                                                                           | Game.css                | Code review and frontend testing                                                                                                    |                         | After a few prompts, we reached a correct visual result                                                                                                                                                   |
| 2.4 | 02/11/2025 21:30 |   2    | Nicolás de Alba León | Chatgpt (GPT5)              | Web           | Asked about CSS styles for the History page                                                                                                                                                                                                  | To have adequate colors with their hovers and so on                                                                                                                   | History.css             | Code review and frontend testing                                                                                                    |                         | With a single prompt, it gave me many things and I removed everything unnecessary                                                                                                                         |
| 2.5 | 29/10/2025 18:17 |   2    | Pablo Romero Gómez   | Chatgpt (GPT5)              | Web           | Make me a background image for a Dixit game on a computer                                                                                                                                                                                    | I wanted a recognizable background image for the lobby screen to verify that, after executing create or join, it joined a lobby. The image use is likely provisional. | Game.js                 | Image visualization.                                                                                                                |                         | The image looks good and I liked it, so I didn't need to ask more about it.                                                                                                                               |
| 2.6 | 02/11/2025 20:45 |   2    | Pablo Romero Gómez   | Chatgpt (GPT5)              | Web           | I asked for help debugging the backend code because the frontend sent the password correctly (when creating the game) but then in the database it appeared as "null", even though the game was created with its id and the player as leader. | Assistance to find a bug in the Game controller in the Post method.                                                                                                   | GameRestController.java | I tested running the code applying the change to a RequestMapping import and saw that the password was indeed saved in the database |                         | It detected it quickly because I narrowed down the problem quite a bit. It turned out to be a RequestMapping import that should have been from Spring instead of Swagger. Since then it worked perfectly. |


### Sprint 3

|   # | Date and Time    | Sprint | Member(s)            | **Tool & Version**           | **Access**                   | **Link to Conversation / Prompt**                                                                                                                                                                                  | **Purpose**                                                                | **Affected Artifacts**                                                                                                                                                                                                                                                                                            | **Human Verification**                              | **Risks & Mitigations** | **Outcome**                                                                                                       |
|----:|------------------|:------:|----------------------|------------------------------|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|-------------------------|-------------------------------------------------------------------------------------------------------------------|
| 3.1 | 24/11/2025 11:03 |   3    | Pablo Romero Gómez   | Github Copilot GPT 5.1 Codex | Visual Studio Code Extension | I attached the TurnService file and asked it to refactor by distributing functionality across entities. I also asked it to change the TurnService method parameters to receive the full object rather than the id. | Refactoring and reorganization of our previously implemented functionality | TurnService, Turn, Game, TurnController, Player, TurnServiceIntegrationTest (calls). Commit: refactor: refactor TurnService with support of generative AI.                                                                                                                                                        | I read the changes and the tests also run correctly |                         | Refactored and much cleaner code, with functionality much more distributed, thus guaranteeing greater modularity. |
| 3.2 | 26/11/2025 12:03 |   3    | Nicolás de Alba León | Gemini 3 Pro                 | Web                          | I attached the Lobby.js and GameRestController files and asked how I could reload the frontend page when a call was made to the backend                                                                            | Added WebSockets for automatic page reloading.                             | Lobby.js, GameRestController, SecurityConfiguration, WebSocketConfig. Commit: So it doesn't reload automatically every two seconds but simply when it receives a certain signal, for example, that another player entered the game. I want to touch the backend as little as possible, and not create new classes | The code works correctly                            |                         | Functional code that reloads the page automatically.                                                              |

### Sprint 4

|   # | Date and Time    | Sprint | Member(s)            | **Tool & Version** | **Access** | **Link to Conversation / Prompt**                                                                                                                                                                        | **Purpose**                                                                               | **Affected Artifacts**                                                                                                                                                                                                                                                                               | **Human Verification**                                                   | **Risks & Mitigations** | **Outcome**                                                         |
|----:|------------------|:------:|----------------------|--------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|-------------------------|---------------------------------------------------------------------|
| 4.1 | 12/01/2026 10:00 |   4    | Nicolás de Alba León | Chat-Gpt Codex     | VS Code    | I attached the project files Game.js, Turn.java and TurnService.java                                                                                                                                     | Create the Game WebSockets, so the page updates the Game alone with the different inputs. | Game.js, TurnService.java Commit: When reloading the page with websockets, I'm trying to do it in Game.js, but it doesn't work correctly, sometimes it doesn't reload like in the turn change, or at the moment of voting, therefore I want you to fix it so it always updates everything perfectly. | The code works correctly, but we have to wait for it to send all sockets |                         | Functional code that reloads the page automatically.                |
| 4.2 | 12/01/2026 12:14 |   4    | Pablo Romero Gómez   | Gemini 3 Pro       | Web        | I asked that, given a frontend for the statistics module made by me without styles, it add styles and organize the layout to look as clean as possible, following the game's theme in the color palette. | Obtain a clear and clean frontend for the statistics module.                              | All frontend components of the statistics module were affected.                                                                                                                                                                                                                                      | The web page displays an adequate interface.                             |                         | Frontend code that works well and achieved the requested interface. |
| 4.3 | 13/01/2026 11:36 |   4    | Pablo Romero Gómez   | Gemini 3 Pro       | Web        | I asked that, given the repository and the statistics service, it create tests for the service.                                                                                                          | Obtain tests for the statistics service                                                   | StatisticsServiceTest.java                                                                                                                                                                                                                                                                           | The tests pass correctly and check the logic adequately.                 |                         | Adequate tests for the statistics module.                           |
| 4.4 | 10/01/2026 18:30 |   4    | Víctor Martínez Trujillo   | Gemini 3 Pro       | Web        | I asked that, given the actual page layout, it would give an appropiate background for different pages in our game (game, home, friends, achievements)                                                                                                          | Obtain background images that tried to fit the game, so a whiteBackground is not left.                                                   | game.css, achievements.css, social.css, home.css, and the adition of the images to the repository.                                                                                                                                                                                                                                                                          | The images are suitable for the game.                 |                         | Adequate images for our aplication.                           |
| 4.5 | 9/01/2026 13 |   4    | Víctor Martínez Trujillo   | Gemini 3 Pro       | Web        | I asked that, given the description of the achievement, it would give an appropiate logo for different kind of achievements in our apliaction (win, play, trick)                                                                                                          | Obtain logo images that tried to fit the achievement they are related to.                                                   | The adition of the images to the repository that are latter called in achievementList.js                                                                                                                                                                                                                                                                          | The logos are suitable for the achievements.                 |                         | Adequate logo for our achievements.                           |
| 4.6 | 10/01/2026 19:15 |   4    | Víctor Martínez Trujillo   | Gemini 3 Pro       | Web        | I asked to generate an icon of a fairy that could be used to differentiate the player who is a storyteller in game                                                                                                          | Obtain logo images that tried to fit the achievement they are related to.                                                   | The adition of the icon to the repository that is latter called in the components of Game.js.                                                                                                                                                                                                                                                                        | The icon is suitable for the game.                 |                         | Adequate icon for our game.                           |
| 4.7 | 11/01/2026 17:00 |   4    | Alonso Gudiel Güemes | GitHub Copilot (GPT-5 mini)    | VS Code    | I asked copilot to help me provide some styles that match Dixit's dark and mysterious aesthetic for pages like user edition, logging in and out and for the layout of the game.                                                                                                                                    | The purpose was to obtain some styles that I could edit by hand to my liking, to make a better looking frontend. | Most of the frontend screens were altered and most of the css files in the frontend/static folder were changed. | The new frontend was tested and everything worked as it should. |                         | A good looking frontend with styles that matches Dixit              |
| 4.8 | 14/01/2026 19:15 |   4    | Víctor Martínez Trujillo   | Copilot       | VS code        | I asked to generate the tests of the TurnController class, following the same scheme I have previously done in FriendController                                                                                                          | Accellerate the adition of tests by creating the TurnControllerTests                                                   | TurnRestControllerTest.java                                                                                                                                                                                                                                                                        | Check that all are passed correctly and the structure is similar to the one provided                 |                         | Adecuate tests for the class|
| 4.9 | 14/01/2026 3 |   4    | Víctor Martínez Trujillo   | Gemini 3 Pro       | Web       | I asked to generate a personalized query which filter by type of achievement, which is an extension of the class achievement.                                                                                                          | Generate a personalized query for AchievementRepository                                                   | AchievementRepository.java                                                                                                                                                                                                                                                                        | Check that the query works as expected                 | |Adecuate query for achievements|


## Final Conclusions on AI Use in the Project
In general, we think AI is a good source of knowledge we can learn from, when it's properly used. In our case, we used it for advanced things and to learn from it. Being sincere, we like the decision of allowing it in the subject since we believe it will be a tool used in our daily work in the future.

## Annex A) Inventory of AI Tools
| Tool | Version/Model | Provider | Access (web/plugin/API) | License/Plan | Observations |
|------|---------------|----------|-------------------------|--------------|--------------|
|   Gemini   |      3         |    Google      |           Web              |      Pro        |       -       |

## Annex B) Glossary of Purposes

Idea/Exploration · Functional Code Generation · Debugging / Diagnosis · Test Generation (unit/integration/e2e) · Technical Design · Technical Documentation (non-narrative) · Refactoring.
- Refactoring
- Generation of images
- Functional Code Generation
- Test Generation

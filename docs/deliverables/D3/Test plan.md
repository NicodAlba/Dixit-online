# Test plan

**Subject:** Diseño y Pruebas (Grado en Ingeniería del Software, Universidad de Sevilla)  
**Academic year:** 2025/2026  
**Group/Team:** LI - 02  
**Project name:** Dixit  
**Repository:** [Link](https://github.com/gii-is-DP1/dp1-2025-2026-li-02)  
**Members:** Nicolás de Alba León (PNS5576 / nicdeleo@alum.us.es), Alonso Gudiel Güemes (XSK0385 / alogudgue@alum.us.es), Víctor Martínez Trujillo (
XCN8537 / vicmartru@alum.us.es), Pablo Romero Gómez (GFQ0792 / pabromgom@alum.us.es), Álvaro Vílchez Alejandre (
NYP0448 / alvvilale@alum.us.es)

## 1. Introduction

This document describes the test plan for the project **Dixit** developed for the subject **Diseño y Pruebas 1** by the
group **LI-02**. The objective of the test plan is guarantee that the developed software fulfills the specified
requirements in the user stories and that the necessary tests have been developed to ensure its performance.

## 2. Scope

The scope of this test plan include:

- Unit tests.
    - Backend unit tests including services / entities / repositories tests.
    - Frontend unit tests: tests of the javascript functions created in frontend.
    - User Interface unit tests: they use the user interface of our frontend components.
- Integration tests. In our case, they are specially controllers tests that will also be executed with JUnit.

## 3. Test Strategy

### 3.1 Types of tests

#### 3.1.1 Unit tests

The unit tests will be realized to ensure the correct performance of the individual components of the software. Test
automatization tools will be used like JUnit in backend and jest in frontend.

#### 3.1.2 Integration tests

The integration tests will be focused on evaluating the interaction between modules or components of the system. We will
realize them at API level, testing our Spring controllers.

## 4. Tools and tests environment

### 4.1 Tools

- **Maven**: Dependencies management and tests execution.
- **JUnit**: Framework of unit tests.
- **JaCoCo**: Generation of code coverage reports. If the command maven install is executed, the report will be copied to
  the repository subfolder /docs/deliverables/D3/coverage (can be visualized clicking on the file index.html of that
  directory).
- **Allure**: Generation of state reports og the last tests executions. It allows to group the tests by module and
  feature. If the command maven install is executed, the report will be copied to the repository subfolder
  /docs/deliverables/D3/coverage (can be visualized clicking on the file index.html of that directory).
- **Jest**: Framework for unit tests in JavaScript.
- **React-test**: Library for the creation of unit tests of React components.

### 4.2 Tests environment

The tests will be executed in the development environment and, eventually, in the tests environment of the continuous
integration server.

## 5. Tests plan

### 5.1 State and traceability of tests by module and epic

The report of tests state (with traceability to the modules and epic/user stories) downloading and opening this [file](
https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/docs/deliverables/D3/status/index.html).

### 5.2 Test Coverage

The tests coverage report can be consulted downloading and opening this [file](
https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/docs/deliverables/D3/coverage/index.html).


| User Stories                                             | Test                                                                                                                                                                                            | Description                                                                                                        | State                              | Type                                    |
|----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|------------------------------------|-----------------------------------------|
| US12: Storyteller's card and clue                        | [storytellerSelectsAnswer](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/TurnTest.java)                                            | Verify that as a storyteller, I can select a card of my hand.                                                      | Implemented                        | Unit in backend of Turn entity          |
| US12: Storyteller's card and clue                        | [storytellersProvidesClue](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/TurnTest.java)                                            | Verify that as a storyteller, I can write a clue.                                                                  | Implemented                        | Unit in backend of Turn entity          |
| US13: Player's card                                      | [registerCardPlay](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/TurnTest.java)                                                    | Verify that as a player I can choose a card of my hand that matches the storyteller's clue.                        | Implemented                        | Unit in backend of Turn entity          |
| US15: Voting and R4: Votation system                     | [registerVote](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/TurnTest.java)                                                        | Verify that as a player I can vote for a card on the table, and that all the votes are correctly counted.          | Implemented                        | Unit in backend of Turn entity          |
| US12, US13 and US15: Turn Actions API                    | All unit tests in [TurnRestControllerTest.java](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/TurnRestControllerTest.java)         | Verify that the API correctly handles all about turns(storyteller clues, playing cards, and voting logic)          | Implemented                        | Integration in backend(Controller)      |
| R5: Scoring System and R6: Extra Points                  | [calculateScoreAdjustments](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/TurnTest.java)                                           | Verify that the scoring logic assigns points to all players correctly.                                             | Implemented                        | Unit in backend of Turn entity          |
| R7: End of turn                                          | [finish_setsStateToFinished](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/TurnTest.java)                                          | Verify that the turn ends (state = FINISHED) when the method Turn.finish() is called                               | Implemented                        | Unit in backend of Turn entity          |
| US21: Game statistics                                    | [findGamesByWinnerUsername](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                    | Verify that I can find past games filtering by winnner's Username.                                                 | Implemented                        | Unit in backend of GameService          |
| US21 and US11: Game statistics and Past games register   | [findFinished()](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                               | Verify that I can find finished games in a proper way.                                                             | Implemented                        | Unit in backend of GameService          |
| US10: Games in progress                                  | [findPlaying](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                                  | Verify that I can find ongoing games.                                                                              | Implemented                        | Unit in backend of GameService          |
| US21: Games statistics                                   | [findPlayersByGameId](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                          | Verify that I can find the players that belong to an ongoing / finished game.                                      | Implemented                        | Unit in backend of GameService          |
| US8: Start game                                          | [minPlayersBeforeStart](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                        | Verify that a game cannot be started if there are not players enough.                                              | Implemented                        | Unit in backend of GameService          |
| R8: End of game                                          | [minPlayersBeforeStart](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                        | Verify that a game cannot be finished if it hasn't started.                                                        | Implemented                        | Unit in backend of GameService          |
| US6 and US7: Create game and join game                   | [findByIdIfUserInGame](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                         | Verify that player are added to games correctly.                                                                   | Implemented                        | Unit in backend of GameService          |
| R8: End of game                                          | [endGameIfNeeded](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                              | Verify that a game ends when it must.                                                                              | Implemented                        | Unit in backend of GameService          |
| US21 and US22: Game statistics and user rankings         | All unit tests in [StatisticsServiceTest.java](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/statistics/StatisticsServiceTest.java)     | Verify that all statistics are computed properly.                                                                  | Implemented                        | Units in  StatisticsServiceTest.java    |
| US23 and US24: Achievements                              | All unit tests in [AchievementServiceTest.java](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/achievement/AchievementServiceTest.java)  | Verify that all achievements are computed properly.                                                                | Implemented                        | Units in  AchievementServiceTest.java   |
| US23 and US24: Locked and Completed Achievements         | Unit test in [AchievementRepositoryTest.java](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/achievement/AchievementRepositoryTest.java) | Verify that the personalized query is executed correctly.                                                          | Implemented                        | Unit in  AchievementRepositoryTest.java |
| US27: Spectate                                           | [addSpectatorsToGame](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                          | Verify that the correct spectators to a game are computed correctly                                                | Implemented                        | Unit in  backend of GameService         |
| US26 and US27: Friend API & Requests                     | All unit tests in [FriendRestControllerTest.java](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/social/FriendRestControllerTest.java)   | Verify that users can manage all friend's logic correctly via API                                                  | Implemented                        | Integration in backend(Controller)      |
| US26 and US27: Friend Service & Social Logic             | All unit tests in [FriendRequestServiceTest.java](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/social/FriendRequestServiceTest.java)   | Verify the business logic for managing friends, including sending, accepting, and removing requests                | Implemented                        | Unit in backend(Service)                |
| US19: Show the winner                                    | [declareWinners](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/XCN8537/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameServiceTest.java)                                            | Verify that the system correctly identifies and declares the player with the most points as the winner             | Implemented                        | Unit in backend(Service)                |
| US25: Send invitations                                   | [testSendInvitation](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/XCN8537/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameRestControllerTest.java)  | Verify that the system correctly send invitations to a game                               | Implemented                                                                                                        | Integration in backend(Controller) |                                         |
| US6, US7, US8 and US27: Game Lifecycle & Invitations API | All unit tests in [GameRestControllerTest.java](https://github.com/gii-is-DP1/dp1-2025-2026-li-02/blob/main/src/test/java/es/us/dp1/lx_xy_24_25/dixit/game/GameRestControllerTest.java)         | Verify the API endpoints for creating, joining, leaving, and starting games, as well as accepting game invitations | Implemented                        | Integration in backend(Controller)      |

## 6. Acceptance criteria
- All unit tests must pass successfully before the final delivery of the project
- Test Coverage must be of at least 70%
- There shouldn't be any critical fails in the integration and functionality tests.

## 7. Conclusion
This test plan establish the structure and criteria to ensure the quality of the developed software. It is responsibility of the developer and test team follor this plan to guarantee the delivery of a functional product and free of errors.

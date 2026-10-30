package es.us.dp1.lx_xy_24_25.dixit.security;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import static org.junit.jupiter.api.Assertions.*;

/** Run explicitly against an isolated PostgreSQL 17 database; never included in H2 runs. */
class PostgresPersistenceIT extends SecurityHttpSupport {
    @Test void migratesReferenceDataAndRestrictsRuntimeRole() throws Exception {
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(17, connection.getMetaData().getDatabaseMajorVersion());
        }
        assertEquals(36, count("select count(*) from images"));
        assertEquals(12, count("select count(*) from badge_images"));
        assertEquals(2, count("select count(*) from authorities"));
        assertEquals(0, count("select count(*) from appusers where username in ('admin1','player1','player2')"));
        assertEquals(false, jdbc.queryForObject("select has_schema_privilege(current_user,'dixit','CREATE')", Boolean.class));
        assertEquals(false, jdbc.queryForObject("select has_table_privilege(current_user,'dixit.images','UPDATE')", Boolean.class));
        assertEquals(false, jdbc.queryForObject("select has_table_privilege(current_user,'dixit.flyway_schema_history','UPDATE')", Boolean.class));
        assertThrows(DataAccessException.class, () -> jdbc.execute("create table dixit.forbidden_runtime_ddl(id integer)"));
        assertThrows(DataAccessException.class, () -> jdbc.update("insert into players(id,user_id) values (-1,-1)"));
    }

    @Test void completesFourPlayersWithReplenishmentHistoryAndPostgresStatistics() throws Exception {
        List<Account> accounts = List.of(account(), account(), account(), account());
        Account host = accounts.getFirst();
        var created = request(HttpMethod.POST, "/games", host.token(), Map.of());
        status(201, created);
        int game = created.getBody().path("id").asInt();
        for (Account actor : accounts.subList(1,4)) status(200, request(HttpMethod.PATCH,"/games/"+game+"/join",actor.token(),Map.of()));
        status(200, request(HttpMethod.PATCH,"/games/"+game+"/start",host.token(),null));
        Map<Integer,Account> players = new LinkedHashMap<>();
        for (Account actor : accounts) players.put(count("select id from players where game_id=? and user_id=?",game,actor.id()),actor);
        for (int round=0; round<8; round++) {
            int turn = count("select max(id) from turns where game_id=?",game);
            int narrator = count("select storyteller_id from turns where id=?",turn);
            Account storyteller = players.get(narrator);
            int answer = count("select min(id) from cards where player_id=? and turn_id is null",narrator);
            String route = "/turns/"+turn;
            status(200,request(HttpMethod.PUT,route+"/answer/"+answer,storyteller.token(),null));
            status(200,request(HttpMethod.PUT,route+"/clue",storyteller.token(),"PostgreSQL fixture"));
            for (var entry : players.entrySet()) if (entry.getKey()!=narrator) {
                int card = count("select min(id) from cards where player_id=? and turn_id is null",entry.getKey());
                status(200,request(HttpMethod.POST,route+"/play-card/"+card+"/"+entry.getKey(),entry.getValue().token(),null));
            }
            for (var entry : players.entrySet()) if (entry.getKey()!=narrator) {
                var view=request(HttpMethod.GET,route,entry.getValue().token(),null); status(200,view);
                assertFalse(view.getBody().hasNonNull("answer"));
                for (var card : view.getBody().path("playedCards")) {
                    assertFalse(card.has("owner")); assertFalse(card.has("votes"));
                }
                status(200,request(HttpMethod.POST,route+"/vote/"+entry.getKey()+"/"+answer,entry.getValue().token(),null));
            }
        }
        for (int player : players.keySet()) {
            assertEquals(2,count("select count(*) from turns where game_id=? and storyteller_id=? and state='FINISHED'",game,player),
                "Each participant narrates exactly twice, independent of PostgreSQL row order");
        }
        assertEquals(8,count("select count(*) from turns where game_id=? and state='FINISHED'",game));
        assertEquals(52,count("select count(*) from cards where game_id=?",game));
        assertEquals(32,count("select count(*) from cards where game_id=? and turn_id is not null and user_id is not null",game));
        assertEquals(24,count("select count(*) from player_votes v join cards c on c.id=v.card_id where c.game_id=?",game));
        assertEquals(20,count("select count(*) from cards where game_id=? and turn_id is null and player_id is not null",game));
        var completed=request(HttpMethod.GET,"/games/"+game,host.token(),null); status(200,completed);
        assertTrue(completed.getBody().hasNonNull("endDate")); assertFalse(completed.getBody().path("cancelled").asBoolean());
        for (Account actor : accounts) {
            var stats=request(HttpMethod.GET,"/stats/user/"+actor.id(),actor.token(),null); status(200,stats);
            assertEquals(1,stats.getBody().path("gamesPlayed").asInt());
            status(200,request(HttpMethod.GET,"/users/"+actor.id()+"/achievements",actor.token(),null));
        }
        java.nio.file.Path evidence = java.nio.file.Path.of("target/postgres-completed-game.json");
        java.nio.file.Files.writeString(evidence, "{\"gameId\":" + game + ",\"token\":\"" + host.token() + "\"}");
        java.nio.file.Files.setPosixFilePermissions(evidence, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        for (String endpoint : List.of("/stats/global", "/stats/rankings/points", "/stats/rankings/wins", "/stats/rankings/avg-points", "/stats/rankings/trap-votes", "/stats/rankings/win-rate", "/stats/rankings/deception-rate")) {
            status(200,request(HttpMethod.GET,endpoint,host.token(),null));
        }
    }
}

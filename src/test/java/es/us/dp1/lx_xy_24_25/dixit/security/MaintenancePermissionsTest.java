package es.us.dp1.lx_xy_24_25.dixit.security;

import es.us.dp1.lx_xy_24_25.dixit.operations.MaintenanceGate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import static org.junit.jupiter.api.Assertions.*;

class MaintenancePermissionsTest extends SecurityHttpSupport {
    @Autowired MaintenanceGate gate;
    @AfterEach void reopen() { gate.setEnabled(false); }
    private static final String CONTROL = "/operations/maintenance";

    @Test void onlyAdminCanInspectOrChangeModeAndPayloadMustBeBooleanOnly() {
        Account player = account(), operator = admin();
        status(401, request(HttpMethod.GET, CONTROL, null, null));
        status(403, request(HttpMethod.GET, CONTROL, player.token(), null));
        status(403, request(HttpMethod.PUT, CONTROL, player.token(), Map.of("enabled", true)));
        assertFalse(gate.isEnabled());
        status(400, request(HttpMethod.PUT, CONTROL, operator.token(), null));
        status(400, request(HttpMethod.PUT, CONTROL, operator.token(), Map.of("enabled", "true")));
        status(400, request(HttpMethod.PUT, CONTROL, operator.token(), Map.of("enabled", true, "gameId", 1)));
        var response = request(HttpMethod.PUT, CONTROL, operator.token(), Map.of("enabled", true));
        status(200, response);
        assertTrue(response.getBody().path("enabled").asBoolean());
        assertEquals(3, response.getBody().size());
    }

    @Test void closedModeBlocksCreateAndStartWithoutChangingLobbyOrActorAuthorization() {
        Account host = account(), second = account(), third = account(), operator = admin();
        var created = request(HttpMethod.POST, "/games", host.token(), Map.of()); status(201, created);
        int game = created.getBody().path("id").asInt();
        for (Account member : List.of(second, third)) status(200, request(HttpMethod.PATCH, "/games/"+game+"/join", member.token(), Map.of()));
        int gamesBefore = count("select count(*) from games");
        int cardsBefore = count("select count(*) from cards where game_id=?", game);
        status(200, request(HttpMethod.PUT, CONTROL, operator.token(), Map.of("enabled", true)));
        var denied = request(HttpMethod.POST, "/games", host.token(), Map.of()); status(503, denied);
        assertEquals("New games are temporarily unavailable for maintenance.", denied.getBody().path("message").asText());
        status(403, request(HttpMethod.PATCH, "/games/"+game+"/start", second.token(), null));
        status(503, request(HttpMethod.PATCH, "/games/"+game+"/start", host.token(), null));
        assertEquals(gamesBefore, count("select count(*) from games"));
        assertEquals(cardsBefore, count("select count(*) from cards where game_id=?", game));
        assertEquals(0, count("select count(*) from turns where game_id=?", game));
        assertEquals(1, count("select count(*) from games where id=? and start_date is null and end_date is null and cancelled=false", game));
        status(200, request(HttpMethod.GET, "/games/"+game, host.token(), null));
        status(200, request(HttpMethod.PUT, CONTROL, operator.token(), Map.of("enabled", false)));
        status(200, request(HttpMethod.PATCH, "/games/"+game+"/start", host.token(), null));
    }
}

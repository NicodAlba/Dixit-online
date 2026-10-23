package es.us.dp1.lx_xy_24_25.dixit.social;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FriendView(Integer id, String username, SpectatableGame spectatableGame) {
    public record SpectatableGame(Integer id, Integer playerId) {}
}

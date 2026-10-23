package es.us.dp1.lx_xy_24_25.dixit.user;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Account metadata, never credentials or participation graphs. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserView(Integer id, String username, Authorities authority) {
    public static UserView of(User user, boolean privateProfile) {
        return new UserView(user.getId(), user.getUsername(), privateProfile ? user.getAuthority() : null);
    }
}

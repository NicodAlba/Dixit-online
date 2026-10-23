package es.us.dp1.lx_xy_24_25.dixit.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
    private String username;

    private String password;

    private Authorities authority;

    public UserDTO(User user) {
        this.username = user.getUsername();
        this.authority = user.getAuthority();
    }
}

package es.us.dp1.lx_xy_24_25.dixit.user;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import es.us.dp1.lx_xy_24_25.dixit.achievement.Achievement;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "appusers")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id")
/*Its required to prevent infinite recursion when serializing, but we cannot
use JsonBackReference and JsonManagedReference because we have a circular relationship.
So we use JsonIdentityInfo that assign an identifier to each entity and use it when serializing references.
*/

@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(optional = false)
    @JoinColumn(name = "authority_id")
    private Authorities authority;

    @JsonIgnore
    @OneToMany(mappedBy = "user", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<Achievement> achievements;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players =  new ArrayList<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "spectators", cascade = CascadeType.ALL)
    List<Game> gamesToSpectate = new ArrayList<>();

    public Boolean hasAuthority(String auth) {
        return authority.getAuthority().equals(auth);
    }

    public Boolean hasAnyAuthority(String... authorities) {
        for (String auth : authorities) {
            if (auth.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

}

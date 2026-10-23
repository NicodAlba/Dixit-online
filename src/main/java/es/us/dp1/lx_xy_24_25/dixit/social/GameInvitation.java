package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "game_invitations", uniqueConstraints = {@UniqueConstraint(columnNames = {"invited_id", "game_id"})})
public class GameInvitation extends BaseEntity {

    @Column
    @NotNull
    @NotBlank
    private String inviter;

    @ManyToOne
    @NotNull
    private User invited;

    @Column(nullable = false)
    @Min(0)
    @NotNull
    private Integer gameId;

    @Column
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;
}

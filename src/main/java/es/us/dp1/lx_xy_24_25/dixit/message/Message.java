package es.us.dp1.lx_xy_24_25.dixit.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "messages")
public class Message extends BaseEntity {

    private Date date;

    private String text;

    @ManyToOne
    private Player player;

    @ManyToOne
    @JsonIgnore
    private Game game;
}

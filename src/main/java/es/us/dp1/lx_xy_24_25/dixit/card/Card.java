package es.us.dp1.lx_xy_24_25.dixit.card;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.Turn;
import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "cards")
public class Card extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "image_id")
    private Image image;

    @ManyToOne
    @JoinColumn(name = "player_id")
    @JsonIgnoreProperties({"game", "cards", "user"})
    private Player owner;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JsonBackReference("game-cards")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "turn_id")
    @JsonBackReference("turn-cards")
    private Turn turn;

    @ManyToMany
    @JoinTable(name = "player_votes", joinColumns = @JoinColumn(name = "card_id"), inverseJoinColumns = @JoinColumn(name = "player_id"))
    @JsonIgnoreProperties({"game", "cards", "user"})
    private List<Player> votes = new ArrayList<>();


    public void vote(Player player) {
        this.votes.add(player);
    }
}

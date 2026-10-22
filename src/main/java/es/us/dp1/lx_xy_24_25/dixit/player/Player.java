package es.us.dp1.lx_xy_24_25.dixit.player;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Getter
@Setter
@Table(name = "players")

public class Player extends BaseEntity {

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "owner")
    private List<Card> cards = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "game_id")
    @JsonBackReference("game-players") //prevents infinite looping during serialization
    private Game game;

    @ManyToOne
    @JoinColumn(name = "player_vote_id")
    private Card playerVote;

    @Column
    @Min(0)
    private Integer points = 0;

    public boolean owns(Card card) {
        return card != null
            && card.getOwner() != null
            && Objects.equals(card.getOwner().getId(), this.getId());
    }

    public void receiveCard(Card card) {
        if (card == null) {
            return;
        }
        card.setOwner(this);
        cards.add(card);
    }

    public void vote(Card card) {
        this.playerVote = card;
    }

    public void addPoints(int delta) {
        if (delta == 0) {
            return;
        }
        if (points == null) {
            points = 0;
        }
        points += delta;
    }
}

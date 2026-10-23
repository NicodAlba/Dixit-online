package es.us.dp1.lx_xy_24_25.dixit.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.message.Message;
import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "games")
public class Game extends BaseEntity {

    @Column(name = "password")
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column
    private Date startDate; // null = Lobby.js / not null = Game.js

    @Column
    private Date endDate;

    //@NotNull was eliminated so the game could be deleted
    @ManyToOne
    @JoinColumn(name = "leader_id")
    private User leader;

    @ManyToMany
    @JoinTable(
        name = "game_winners",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private List<Player> winners = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("game-turns") //prevents infinite looping during serialization
    private List<Turn> turns = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("game-players") //prevents infinite looping during serialization
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Message> messages = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("game-cards") //prevents infinite looping during serialization
    // All card occurrences, including history; CardService calculates drawable images.
    private List<Card> deck = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "game_spectators",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> spectators = new ArrayList<>();

    public void setEndDate(Date endDate) throws IllegalStateException{
        if (startDate == null) {
            throw new IllegalStateException("The game hasn't started yet");
        }
        this.endDate = endDate;
    }

    public void setStartDate(Date startDate) throws IllegalStateException {
        if (players.size() < 3) {
            throw new IllegalStateException("Too few players");
        }
        if (players.size() > 4) {
            throw new IllegalStateException("Too many players");
        }
        this.startDate = startDate;
    }

    public List<Player> playersExcept(Player storyteller) {
        if (players == null || players.isEmpty()) {
            return new ArrayList<>();
        }
        return players.stream()
            .filter(player -> storyteller == null || !Objects.equals(player.getId(), storyteller.getId()))
            .collect(Collectors.toList());
    }

    public Player nextStoryteller(Player currentStoryteller) {
        if (players == null || players.isEmpty()) {
            throw new IllegalStateException("Game does not have players");
        }
        if (currentStoryteller == null) {
            return players.get(0);
        }
        int currentIndex = players.indexOf(currentStoryteller);
        if (currentIndex < 0) {
            throw new IllegalStateException("Storyteller is not part of this game");
        }
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex);
    }

    public boolean hasFinished() {
        return endDate != null;
    }

    public List<Card> dealCards(List<Card> deck, int cardsPerPlayer) {
        List<Card> dealtCards = new ArrayList<>();
        if (deck == null || deck.isEmpty() || cardsPerPlayer <= 0) {
            return dealtCards;
        }
        for (Player player : players) {
            for (int i = 0; i < cardsPerPlayer && !deck.isEmpty(); i++) {
                Card card = deck.removeFirst();
                player.receiveCard(card);
                dealtCards.add(card);
            }
        }
        return dealtCards;
    }
}

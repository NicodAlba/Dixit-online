package es.us.dp1.lx_xy_24_25.dixit.game;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.model.BaseEntity;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "turns")
public class Turn extends BaseEntity {

    @JsonIgnore
    @Column(name = "table_order_seed", length = 64)
    private String tableOrderSeed = UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "answer_id")
    private Card answer;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private TurnState state;

    @Column(name = "clue")
    private String clue;

    @ManyToOne
    @JoinColumn(name = "storyteller_id")
    private Player storyteller;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "game_id")
    @JsonBackReference("game-turns")
    private Game game;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "turn_id")
    @JsonManagedReference("turn-cards") 
    private List<Card> playedCards = new ArrayList<>();

    public void storytellerSelectsAnswer(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Storyteller card must not be null");
        }
        if (storyteller != null && !storyteller.owns(card)) {
            throw new IllegalArgumentException("Storyteller must play one of their cards");
        }
        if (answer != null) {
            throw new IllegalStateException("Storyteller can not play more than one card");
        }
        if (storyteller != null) {
            storyteller.getCards().remove(card);
        }
        card.setTurn(this);
        this.answer = card;
        this.playedCards.add(card);
        this.state = TurnState.WAITING_STORYTELLER_CLUE;

    }

    public void storytellerProvidesClue(String clue) {
        this.clue = clue;
        this.state = TurnState.WAITING_PLAYER_CARDS;
    }

    public void registerCardPlay(Card card, Player player) {
        validatePlayerAndCardNotNull(player, card);
        validateCardOwnership(card, player);
        validatePlayerHasntPlayedCard(player);
        card.setTurn(this);
        playedCards.add(card);
        player.getCards().remove(card);
    }

    public boolean registerVote(Player player, Card card) {
        validatePlayerAndCardNotNull(player, card);
        validateCardBelongsToTurn(card);
        validatePlayerHasntVoted(player);
        card.vote(player);
        return hasAllVotesFromPlayers();
    }

    public List<Player> getNonStorytellers() {
        if (game == null) {
            return Collections.emptyList();
        }
        return game.playersExcept(storyteller);
    }

    public boolean allPlayersChoseCard() {
        long playersWhoPlayed =
            playedCards.stream()
                .map(Card::getOwner)
                .filter(Objects::nonNull)
                .map(Player::getId)
                .distinct()
                .count();

        return playersWhoPlayed == game.getPlayers().size();
    }

    public boolean hasAllVotesFromPlayers() {
        int expectedVotes = getNonStorytellers().size();
        int votes = playedCards.stream().mapToInt(c -> c.getVotes().size()).sum();
        return votes == expectedVotes;
    }

    public void validateCardBelongsToTurn(Card card) {
        if (playedCards.stream()
            .noneMatch((played -> Objects.equals(played.getId(), card.getId())))) {
            throw new IllegalArgumentException("Card with id " + card.getId() + " does is not an option for this turn");
            }
    }

    public Map<Player, Integer> calculateScoreAdjustments() {
        if (answer == null) {
            throw new IllegalStateException("Storyteller card must be set before scoring");
        }
        List<Player> playersNotStoryteller = getNonStorytellers();
        Map<Card, Integer> votesPerCard = countVotes();
        Integer votesForStorytellerCard = votesPerCard.get(answer);
        boolean everyoneGuessed = votesForStorytellerCard == playersNotStoryteller.size();
        boolean noOneGuessed = votesForStorytellerCard == 0;

        Map<Player, Integer> pointAdjustments = new HashMap<>();

        if (everyoneGuessed || noOneGuessed) {
            playersNotStoryteller.forEach(player -> pointAdjustments.merge(player, 2, Integer::sum));
        } else {
            pointAdjustments.merge(storyteller, 3, Integer::sum);
            playedCards.stream()
                .filter(card -> {
                    List<Player> whoVoted = card.getVotes();
                    return !whoVoted.isEmpty() && Objects.equals(card.getId(), answer.getId());
                }).flatMap(card -> card.getVotes().stream())
                .forEach(player -> pointAdjustments.merge(player, 3, Integer::sum));
        }

        votesPerCard.forEach((card, voteCount) -> {
            if (card == null || Objects.equals(card.getId(), answer.getId())) {
                return;
            }
            Player cardOwner = card.getOwner();
            if (cardOwner != null && (storyteller == null || !Objects.equals(cardOwner.getId(), storyteller.getId()))) {
                pointAdjustments.merge(cardOwner, voteCount, Integer::sum);
            }
        });

        return pointAdjustments;
    }

    public void finish() {
        this.state = TurnState.FINISHED;
    }

    private Map<Card, Integer> countVotes() {
        Map<Card, Integer> votes = playedCards.stream().collect(Collectors.toMap(c->c, c-> c.getVotes().size()));
        votes.put(answer, answer.getVotes().size());
        return votes;
    }

    void validateCardOwnership(Card card, Player player) {
        if (player == null || !player.owns(card)) {
            throw new IllegalArgumentException("Card does not belong to the player");
        }
    }
    void validatePlayerAndCardNotNull(Player player, Card card) {
        if (player == null || card == null) {
            throw new IllegalArgumentException("Player and card must not be null");
        }
    }

    void validatePlayerHasntPlayedCard(Player player) {
        if (this.getPlayedCards().stream().anyMatch(c -> c.getOwner().equals(player))) {
            throw new IllegalStateException("You have already played a card");
        }
    }

    void validatePlayerHasntVoted(Player player) {
        if (this.getPlayedCards().stream().anyMatch(c -> c.getVotes().stream().anyMatch(p -> p.equals(player)))) {
            throw new IllegalStateException("You have already voted a card");
        }
    }

}

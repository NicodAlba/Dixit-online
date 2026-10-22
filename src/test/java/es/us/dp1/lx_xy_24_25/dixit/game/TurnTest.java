package es.us.dp1.lx_xy_24_25.dixit.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;

@Transactional
class TurnTest {

    private Game game;
    private Turn turn;
    private Player storyteller;
    private Player playerTwo;
    private Player playerThree;

    @BeforeEach
    void setUp() {
        game = new Game();

        storyteller = buildPlayer(1);
        playerTwo = buildPlayer(2);
        playerThree = buildPlayer(3);
        game.setPlayers(new ArrayList<>(Arrays.asList(storyteller, playerTwo, playerThree)));

        Card storytellerCard = storyteller.getCards().getFirst();
        turn = new Turn();
        turn.setGame(game);
        turn.setStoryteller(storyteller);
        turn.setAnswer(storytellerCard);
        turn.getPlayedCards().add(storytellerCard);
    }

    //helper functions for setup
    private Player buildPlayer(int id) {
        Player player = new Player();
        player.setId(id);
        player.setPoints(0);
        player.setGame(game);
        player.setCards(new ArrayList<>());
        dealHand(player);
        return player;
    }

    private void dealHand(Player player) {
        for (int i = 0; i < 6; i++) {
            Card card = new Card();
            card.setId(player.getId() * 100 + i);
            card.setOwner(player);
            card.setGame(game);
            player.getCards().add(card);
            game.getDeck().add(card);
        }
    }

    private void playCardsForPlayers() {
        Card playerTwoCard = playerTwo.getCards().getFirst();
        turn.registerCardPlay(playerTwoCard, playerTwo);
        Card playerThreeCard = playerThree.getCards().getFirst();
        turn.registerCardPlay(playerThreeCard, playerThree);
    }

    private Turn newTurnForStoryteller() {
        Turn freshTurn = new Turn();
        freshTurn.setGame(game);
        freshTurn.setStoryteller(storyteller);
        return freshTurn;
    }

    @Test
    void storytellerSelectsAnswer_updatesStateAndCollection() {
        Card newStorytellerCard = storyteller.getCards().get(1);
        Turn freshTurn = newTurnForStoryteller();
        freshTurn.storytellerSelectsAnswer(newStorytellerCard);

        assertEquals(newStorytellerCard, freshTurn.getAnswer());
        assertTrue(freshTurn.getPlayedCards().contains(newStorytellerCard));
    }

    @Test
    void storytellerSelectsAnswer_rejectsNonOwnedCard() {
        Card foreignCard = playerTwo.getCards().getFirst();
        assertThrows(IllegalArgumentException.class, () -> turn.storytellerSelectsAnswer(foreignCard));
    }

    @Test
    void storytellerProvidesClue_setsClueAndState() {
        String clue = "A mysterious journey";
        turn.storytellerProvidesClue(clue);

        assertEquals(clue, turn.getClue());
        assertEquals(TurnState.WAITING_PLAYER_CARDS, turn.getState());
    }

    @Test
    void registerCardPlay_removesCardFromPlayerAndDetectsCompletion() {
        Card playerTwoCard = playerTwo.getCards().getFirst();
        turn.registerCardPlay(playerTwoCard, playerTwo);
        assertTrue(turn.getPlayedCards().contains(playerTwoCard));
        assertFalse(playerTwo.getCards().contains(playerTwoCard));
        assertFalse(turn.allPlayersChoseCard());

        Card playerThreeCard = playerThree.getCards().getFirst();
        turn.registerCardPlay(playerThreeCard, playerThree);
        assertTrue(turn.getPlayedCards().contains(playerThreeCard));
        assertFalse(playerThree.getCards().contains(playerThreeCard));
        assertTrue(turn.allPlayersChoseCard());
    }

    @Test
    void registerCardPlay_rejectsNonExistingCard() {
        Card invalidCard = new Card();
        invalidCard.setId(999);
        assertThrows(IllegalArgumentException.class, () -> turn.registerCardPlay(invalidCard, playerTwo));
    }

    @Test
    void registerVote_requiresCardInTurn() {
        Card foreignCard = playerTwo.getCards().getFirst();
        assertThrows(IllegalArgumentException.class, () -> turn.registerVote(playerTwo, foreignCard));
    }

    @Test
    void registerVote_marksWhenEveryoneVoted() {
        playCardsForPlayers();
        boolean readyForScoring = turn.registerVote(playerTwo, turn.getAnswer());
        assertFalse(readyForScoring);

        readyForScoring = turn.registerVote(playerThree, turn.getAnswer());
        assertTrue(readyForScoring);
        assertTrue(turn.getAnswer().getVotes().containsAll(Arrays.asList(playerTwo, playerThree)));
    }

    @Test
    void calculateScoreAdjustments_everyoneGuesses_awardsTwoPointsEach() {
        playCardsForPlayers();
        turn.getAnswer().vote(playerTwo);
        turn.getAnswer().vote(playerThree);

        Map<Player, Integer> adjustments = turn.calculateScoreAdjustments();

        assertEquals(2, adjustments.get(playerTwo));
        assertEquals(2, adjustments.get(playerThree));
        assertFalse(adjustments.containsKey(storyteller));
    }

    @Test
    void calculateScoreAdjustments_noOneGuesses() {
        playCardsForPlayers();
        Card bluffCardTwo = turn.getPlayedCards().get(1);
        Card bluffCardThree = turn.getPlayedCards().get(2);
        bluffCardTwo.vote(playerTwo);
        bluffCardThree.vote(playerThree);

        Map<Player, Integer> adjustments = turn.calculateScoreAdjustments();

        assertEquals(3, adjustments.get(playerTwo));
        assertEquals(3, adjustments.get(playerThree));
        assertFalse(adjustments.containsKey(storyteller));
    }

    @Test
    void calculateScoreAdjustments_mixedGuesses_rewardsStorytellerAndCardOwners() {
        playCardsForPlayers();
        turn.getAnswer().vote(playerTwo);
        Card bluffCard = turn.getPlayedCards().get(2);
        bluffCard.vote(playerThree);

        Map<Player, Integer> adjustments = turn.calculateScoreAdjustments();

        assertEquals(3, adjustments.get(storyteller));
        assertEquals(3, adjustments.get(playerTwo));
        assertEquals(1, adjustments.get(playerThree));
    }

    @Test
    void finish_setsStateToFinished() {
        turn.finish();
        assertEquals(TurnState.FINISHED, turn.getState());
    }

}

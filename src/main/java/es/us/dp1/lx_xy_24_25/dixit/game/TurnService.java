package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.card.CardRepository;
import es.us.dp1.lx_xy_24_25.dixit.card.CardService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class TurnService {
    private final TurnRepository turnRepository;
    private final CardRepository cardRepository;
    private final PlayerRepository playerRepository;
    private final GameService gameService;
    private final CardService cardService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public TurnService(TurnRepository turnRepository, CardRepository cardRepository, PlayerRepository playerRepository,
                       GameService gameService, CardService cardService, SimpMessagingTemplate messagingTemplate) {
        this.turnRepository = turnRepository;
        this.cardRepository = cardRepository;
        this.playerRepository = playerRepository;
        this.gameService = gameService;
        this.cardService = cardService;
        this.messagingTemplate = messagingTemplate;
    }
    private void notifyGameUpdate(Game game) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + game.getId(),
            game
        );
    }

    private void notifyTurnUpdate(Turn turn) {
        messagingTemplate.convertAndSend(
            "/topic/turn/" + turn.getId(),
            turn
        );
    }
    @Transactional(readOnly = true)
    public Turn findById(Integer id) {
        return turnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Turn", "id", id));
    }

    @Transactional(readOnly = true)
    public Player findPlayerById(Integer playerId) {
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new ResourceNotFoundException("Player", "id", playerId));
    }

    @Transactional
    public Turn setStorytellerAnswer(Turn turn, Card card) {
        turn.storytellerSelectsAnswer(card);
        cardRepository.save(card);
        if (turn.getStoryteller() != null) {
            playerRepository.save(turn.getStoryteller());
        }
        Turn saved = turnRepository.save(turn);

        notifyTurnUpdate(saved);
        notifyGameUpdate(turn.getGame());

        return saved;
    }

    @Transactional
    public Turn setStorytellerClue(Turn turn, String clue) throws IllegalArgumentException {
        if (turn.getClue() != null) {
            throw new IllegalStateException("Already set clue");
        }
        if (clue.isBlank()) {
            throw new IllegalArgumentException("Clue can't be empty");
        }
        turn.setClue(clue);
        turn.setState(TurnState.WAITING_PLAYER_CARDS);

        Turn saved = turnRepository.save(turn);

        notifyTurnUpdate(saved);
        notifyGameUpdate(turn.getGame());

        return saved;
    }


    @Transactional
    public void initializeFirstTurn(Game game) {
        Turn turn = new Turn();
        turn.setGame(game);

        Player storyteller = selectRandomStoryteller(game.getPlayers());
        turn.setStoryteller(storyteller);
        turn.setState(TurnState.WAITING_STORYTELLER_CARD);

        List<Card> deck = cardService.initializeDeck(game);

        List<Card> dealtCards = game.dealCards(deck, 6);
        persistDealtCards(dealtCards);

        game.getTurns().add(turn);
        turnRepository.save(turn);
    }

    @Transactional(rollbackFor = IllegalStateException.class)
    public Card playerPlayCard(Turn turn, Player player, Card card) {
        turn.validatePlayerHasntPlayedCard(player);
        turn.validateCardOwnership(card, player);
        turn.registerCardPlay(card, player);

        cardRepository.save(card);
        playerRepository.save(player);

        if (turn.allPlayersChoseCard()) {
            turn.setState(TurnState.SHOWING_CARDS);
        }
        Turn saved = turnRepository.save(turn);

        notifyTurnUpdate(saved);
        notifyGameUpdate(turn.getGame());

        return card;
    }

    @Transactional
    public Card playerVote(Turn turn, Player player, Card card) {
        turn.validateCardBelongsToTurn(card);
        turn.validatePlayerHasntVoted(player);
        boolean readyForScoring = turn.registerVote(player, card);
        Card saved = cardRepository.save(card);
        playerRepository.save(player);

        if (readyForScoring) {
            turn.setState(TurnState.SCORING);
        }
        Turn savedTurn = turnRepository.save(turn);
        notifyTurnUpdate(savedTurn);

        if (turn.getState() == TurnState.SCORING) {
            scoreTurn(savedTurn);
        }
        if (turn.getState() == TurnState.FINISHED) {
            notifyTurnUpdate(savedTurn);
            Game game = savedTurn.getGame();
            if (game != null && !game.hasFinished()) {
                moveToNextTurn(savedTurn);
            }
            notifyGameUpdate(game);
        }


        return saved;
    }

    //Scoring method
    @Transactional
    public void scoreTurn(Turn turn) {
        Map<Player, Integer> pointAdjustments = turn.calculateScoreAdjustments();
        pointAdjustments.forEach((player, delta) -> {
            player.addPoints(delta);
            playerRepository.save(player);
        });

        turn.getPlayedCards().forEach(card -> {
            card.setUser(card.getOwner().getUser());
            card.setOwner(null);
        });

        turn.finish();
        Turn saved = turnRepository.save(turn);

        notifyTurnUpdate(saved);

        notifyGameUpdate(turn.getGame());

        Game game = turn.getGame();
        if (game != null) {
            gameService.endGameIfNeeded(game, turn);
            if (game.getEndDate() != null) {
                gameService.declareWinners(game);
            }
        }

    }

    private Player selectRandomStoryteller(List<Player> players) {
        if (players == null || players.isEmpty()) {
            throw new IllegalStateException("Cannot start a turn without players");
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(players.size());
        return players.get(randomIndex);
    }

    private void persistDealtCards(List<Card> dealtCards) {
        if (dealtCards == null || dealtCards.isEmpty()) {
            return;
        }
        cardRepository.saveAll(dealtCards);
        Set<Player> updatedPlayers = dealtCards.stream()
            .map(Card::getOwner)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (!updatedPlayers.isEmpty()) {
            playerRepository.saveAll(updatedPlayers);
        }
    }

    @Transactional
    public void moveToNextTurn(Turn currentTurn) {
        Game game = currentTurn.getGame();
        if (game == null) {
            throw new IllegalStateException("Game not found for the current turn");
        }
        if (game.hasFinished()) {
            throw new IllegalStateException("Cannot start a new turn because the game has already finished");
        }
        Turn newTurn = new Turn();
        newTurn.setGame(game);
        Player nextStoryteller = game.nextStoryteller(currentTurn.getStoryteller());
        newTurn.setStoryteller(nextStoryteller);
        newTurn.setState(TurnState.WAITING_STORYTELLER_CARD);
        // Deal one card to each player if possible
        if (cardsEnoughToDeal(game)) {
            List<Card> availableCards = cardRepository.findByOwnerIsNullAndTurnIsNullAndGameId(game.getId());
            List<Card> dealtCards = game.dealCards(availableCards, 1);
            persistDealtCards(dealtCards);
        }
        game.getTurns().add(newTurn);
        turnRepository.save(newTurn);
    }

    @Transactional(readOnly = true)
    public Boolean cardsEnoughToDeal(Game game) {
        int cardsNeeded = game.getPlayers().size();
        int availableCards = cardRepository.countByGameAndOwnerIsNull(game);
        return availableCards >= cardsNeeded;
    }

    @Transactional(readOnly = true)
    public Card findCardById(Integer cardId) {
        return cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
    }

}

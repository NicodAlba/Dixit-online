package es.us.dp1.lx_xy_24_25.dixit.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.card.CardRepository;
import es.us.dp1.lx_xy_24_25.dixit.card.CardService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Date;
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
    private final ObjectMapper objectMapper;

    @Autowired
    public TurnService(TurnRepository turnRepository, CardRepository cardRepository, PlayerRepository playerRepository,
                       GameService gameService, CardService cardService, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.turnRepository = turnRepository;
        this.cardRepository = cardRepository;
        this.playerRepository = playerRepository;
        this.gameService = gameService;
        this.cardService = cardService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
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
    public Game startGame(Integer gameId, User user) {
        Game game = gameService.findByIdForUpdate(gameId);
        if (game.getLeader() == null || !game.getLeader().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the leader can start the game");
        }
        gameService.ensureLobbyOpen(game);
        game.setStartDate(new Date());
        gameService.addSpectatorsToGame(game);
        initializeFirstTurn(game);
        return game;
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
        if (dealtCards.size() != game.getPlayers().size() * 6) {
            throw new IllegalStateException("Not enough cards to deal six to every player");
        }
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
    public Card playerVote(Integer turnId, Integer playerId, Integer cardId) {
        Game game = lockGameForTurn(turnId);
        Turn turn = findById(turnId);
        Player player = findPlayerById(playerId);
        Card card = findCardById(cardId);
        if (game.hasFinished() || (turn.getState() != TurnState.SHOWING_CARDS && turn.getState() != TurnState.VOTING)) {
            throw new IllegalStateException("Turn is not accepting votes");
        }
        turn.validateCardBelongsToTurn(card);
        turn.validatePlayerHasntVoted(player);
        boolean readyForScoring = turn.registerVote(player, card);
        Card saved = cardRepository.save(card);
        playerRepository.save(player);
        if (readyForScoring) {
            turn.setState(TurnState.SCORING);
            scoreTurn(turn);
            if (!game.hasFinished()) advanceFinishedTurn(turn, game);
        }
        turnRepository.save(turn);
        publishCommittedVote(turn, readyForScoring);
        return saved;
    }

    @Transactional
    public Card playerVote(Turn turn, Player player, Card card) {
        return playerVote(turn.getId(), player.getId(), card.getId());
    }

    private Game lockGameForTurn(Integer turnId) {
        // This scalar lookup avoids loading stale entities before acquiring the game lock.
        Integer gameId = turnRepository.findGameIdById(turnId)
            .orElseThrow(() -> new ResourceNotFoundException("Turn", "id", turnId));
        return gameService.findByIdForUpdate(gameId);
    }

    private void publishCommittedVote(Turn turn, boolean gameChanged) {
        // Snapshot while associations are attached; publish JSON only after a successful commit.
        JsonNode turnPayload = objectMapper.valueToTree(turn);
        JsonNode gamePayload = gameChanged ? objectMapper.valueToTree(turn.getGame()) : null;
        Integer turnId = turn.getId(), gameId = turn.getGame().getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/turn/" + turnId, turnPayload);
                if (gamePayload != null) messagingTemplate.convertAndSend("/topic/game/" + gameId, gamePayload);
            }
        });
    }

    //Scoring method
    @Transactional
    public void scoreTurn(Turn turn) {
        if (turn.getState() == TurnState.FINISHED) return;
        Map<Player, Integer> pointAdjustments = turn.calculateScoreAdjustments();
        pointAdjustments.forEach((player, delta) -> {
            player.addPoints(delta);
            playerRepository.save(player);
        });

        turn.getPlayedCards().forEach(card -> {
            Player owner = card.getOwner();
            card.setUser(owner.getUser());
            owner.getCards().remove(card);
            card.setOwner(null);
        });

        turn.finish();
        turnRepository.save(turn);

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
        Game game = lockGameForTurn(currentTurn.getId());
        advanceFinishedTurn(findById(currentTurn.getId()), game);
    }

    private void advanceFinishedTurn(Turn currentTurn, Game game) {
        Turn latest = turnRepository.findFirstByGameIdOrderByIdDesc(game.getId()).orElseThrow();
        if (!Objects.equals(latest.getId(), currentTurn.getId()) || game.hasFinished()) return;
        if (currentTurn.getState() != TurnState.FINISHED) throw new IllegalStateException("Only a finished turn can advance");
        cardService.replenishHands(game);
        Turn newTurn = new Turn();
        newTurn.setGame(game);
        newTurn.setStoryteller(game.nextStoryteller(currentTurn.getStoryteller()));
        newTurn.setState(TurnState.WAITING_STORYTELLER_CARD);
        game.getTurns().add(newTurn);
        turnRepository.save(newTurn);
    }

    @Transactional(readOnly = true)
    public Boolean cardsEnoughToDeal(Game game) {
        return cardService.countAvailableImages(game) >= game.getPlayers().size();
    }

    @Transactional(readOnly = true)
    public Card findCardById(Integer cardId) {
        return cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));
    }

}

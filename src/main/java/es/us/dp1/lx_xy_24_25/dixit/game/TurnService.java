package es.us.dp1.lx_xy_24_25.dixit.game;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.card.CardRepository;
import es.us.dp1.lx_xy_24_25.dixit.card.CardService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.InvalidRequestException;
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
    private final UserService userService;

    @Autowired
    public TurnService(TurnRepository turnRepository, CardRepository cardRepository, PlayerRepository playerRepository,
                       GameService gameService, CardService cardService, SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.turnRepository = turnRepository;
        this.cardRepository = cardRepository;
        this.playerRepository = playerRepository;
        this.gameService = gameService;
        this.cardService = cardService;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
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

    @Transactional(readOnly = true)
    public Turn findVisibleById(Integer id) {
        Turn turn = findById(id);
        gameService.findByIdIfUserInGame(turn.getGame().getId(), userService.findCurrentUser());
        return turn;
    }

    private Player currentActor(Game game, Integer requestedPlayerId) {
        Integer userId = userService.findCurrentUser().getId();
        Player actor = game.getPlayers().stream().filter(p -> Objects.equals(p.getUser().getId(), userId))
            .findFirst().orElseThrow(() -> new AccessDeniedException("You are not a participant in this game."));
        if (requestedPlayerId != null && !Objects.equals(actor.getId(), requestedPlayerId)) {
            throw new AccessDeniedException("You cannot act as another participant.");
        }
        return actor;
    }

    private void requireCurrentTurn(Game game, Turn turn, TurnState... allowed) {
        Turn latest = turnRepository.findFirstByGameIdOrderByIdDesc(game.getId()).orElseThrow();
        if (game.getStartDate() == null || game.hasFinished() || !Objects.equals(latest.getId(), turn.getId())
                || java.util.Arrays.stream(allowed).noneMatch(state -> state == turn.getState())) {
            throw new InvalidRequestException("This action is not allowed in the current turn phase.");
        }
    }

    private void requireOwnUnplayedCard(Game game, Player player, Card card) {
        if (card.getGame() == null || !Objects.equals(card.getGame().getId(), game.getId()) || !player.owns(card)) {
            throw new AccessDeniedException("You must select your own card from this game.");
        }
        if (card.getTurn() != null) throw new InvalidRequestException("This card has already been played.");
    }

    @Transactional
    public Turn selectAnswer(Integer turnId, Integer cardId) {
        Game game = lockGameForTurn(turnId);
        Turn turn = findById(turnId);
        Player actor = currentActor(game, null);
        if (!Objects.equals(actor.getId(), turn.getStoryteller().getId())) throw new AccessDeniedException("Only the storyteller selects the answer.");
        requireCurrentTurn(game, turn, TurnState.WAITING_STORYTELLER_CARD);
        Card card = findCardById(cardId);
        requireOwnUnplayedCard(game, actor, card);
        return setStorytellerAnswer(turn, card);
    }

    @Transactional
    public Turn provideClue(Integer turnId, String clue) {
        Game game = lockGameForTurn(turnId);
        Turn turn = findById(turnId);
        Player actor = currentActor(game, null);
        if (!Objects.equals(actor.getId(), turn.getStoryteller().getId())) throw new AccessDeniedException("Only the storyteller provides the clue.");
        requireCurrentTurn(game, turn, TurnState.WAITING_STORYTELLER_CLUE);
        if (clue == null || clue.isBlank()) throw new InvalidRequestException("Clue cannot be empty.");
        return setStorytellerClue(turn, clue);
    }

    @Transactional
    public Card playCard(Integer turnId, Integer playerId, Integer cardId) {
        Game game = lockGameForTurn(turnId);
        Turn turn = findById(turnId);
        Player actor = currentActor(game, playerId);
        if (Objects.equals(actor.getId(), turn.getStoryteller().getId())) throw new AccessDeniedException("The storyteller has already selected the answer.");
        requireCurrentTurn(game, turn, TurnState.WAITING_PLAYER_CARDS);
        Card card = findCardById(cardId);
        requireOwnUnplayedCard(game, actor, card);
        if (turn.getPlayedCards().stream().anyMatch(c -> c.getOwner() != null && Objects.equals(c.getOwner().getId(), actor.getId()))) {
            throw new InvalidRequestException("You have already played a card.");
        }
        return playerPlayCard(turn, actor, card);
    }

    @Transactional
    Turn setStorytellerAnswer(Turn turn, Card card) {
        turn.storytellerSelectsAnswer(card);
        cardRepository.save(card);
        if (turn.getStoryteller() != null) {
            playerRepository.save(turn.getStoryteller());
        }
        Turn saved = turnRepository.save(turn);

        publishCommittedVote(saved, true);

        return saved;
    }

    @Transactional
    Turn setStorytellerClue(Turn turn, String clue) throws IllegalArgumentException {
        if (turn.getClue() != null) {
            throw new IllegalStateException("Already set clue");
        }
        if (clue.isBlank()) {
            throw new IllegalArgumentException("Clue can't be empty");
        }
        turn.setClue(clue);
        turn.setState(TurnState.WAITING_PLAYER_CARDS);

        Turn saved = turnRepository.save(turn);

        publishCommittedVote(saved, true);

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
    Card playerPlayCard(Turn turn, Player player, Card card) {
        turn.validatePlayerHasntPlayedCard(player);
        turn.validateCardOwnership(card, player);
        turn.registerCardPlay(card, player);

        cardRepository.save(card);
        playerRepository.save(player);

        if (turn.allPlayersChoseCard()) {
            turn.setState(TurnState.SHOWING_CARDS);
        }
        Turn saved = turnRepository.save(turn);

        publishCommittedVote(saved, true);

        return card;
    }

    @Transactional
    public Card playerVote(Integer turnId, Integer playerId, Integer cardId) {
        Game game = lockGameForTurn(turnId);
        Player actor = currentActor(game, playerId);
        return recordVote(game, findById(turnId), actor, findCardById(cardId));
    }

    // Package-local domain entry for transactional scoring fixtures, never a REST command.
    @Transactional
    Card playerVote(Turn turn, Player player, Card card) {
        Game game = lockGameForTurn(turn.getId());
        return recordVote(game, findById(turn.getId()), findPlayerById(player.getId()), findCardById(card.getId()));
    }

    private Card recordVote(Game game, Turn turn, Player player, Card card) {
        if (game.getPlayers().stream().noneMatch(p -> Objects.equals(p.getId(), player.getId()))
                || Objects.equals(player.getId(), turn.getStoryteller().getId())) throw new AccessDeniedException("Only non-storyteller participants can vote.");
        requireCurrentTurn(game, turn, TurnState.SHOWING_CARDS, TurnState.VOTING);
        if (card.getTurn() == null || !Objects.equals(card.getTurn().getId(), turn.getId())) throw new InvalidRequestException("Card is not on this turn's table.");
        if (card.getOwner() != null && Objects.equals(card.getOwner().getId(), player.getId())) throw new AccessDeniedException("You cannot vote for your own card.");
        if (turn.getPlayedCards().stream().flatMap(c -> c.getVotes().stream()).anyMatch(p -> Objects.equals(p.getId(), player.getId()))) {
            throw new InvalidRequestException("You have already voted.");
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
    private Game lockGameForTurn(Integer turnId) {
        // This scalar lookup avoids loading stale entities before acquiring the game lock.
        Integer gameId = turnRepository.findGameIdById(turnId)
            .orElseThrow(() -> new ResourceNotFoundException("Turn", "id", turnId));
        return gameService.findByIdForUpdate(gameId);
    }

    private void publishCommittedVote(Turn turn, boolean gameChanged) {
        Integer turnId=turn.getId(), gameId=turn.getGame().getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/turn/"+turnId,GameEvent.turn(gameId,turnId));
                if(gameChanged) messagingTemplate.convertAndSend("/topic/game/"+gameId,GameEvent.changed(gameId));
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

package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.achievement.AchievementService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.PlayerAlreadyInGameException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.TooManyPlayersException;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerService;
import es.us.dp1.lx_xy_24_25.dixit.social.FriendRequestService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final PlayerService playerService;
    private final UserService userService;
    private final AchievementService achievementService;
    private final FriendRequestService friendRequestService;

    @Autowired
    public GameService(GameRepository gameRepository, PlayerService playerService, UserService userService, AchievementService achievementService, FriendRequestService friendRequestService) {
        this.gameRepository = gameRepository;
        this.playerService = playerService;
        this.userService = userService;
        this.achievementService = achievementService;
        this.friendRequestService = friendRequestService;
    }
    //CRUD
    @Transactional(readOnly = true)
    public Iterable<Game> findAll() {
        return gameRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Game findById(Integer id){
        return gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", id));
    }

    @Transactional(readOnly = true)
    public Game findByIdIfUserInGame(Integer gameId, User user) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));
        if (playerInGame(game, user) || playerSpectating(game, user)) {
            return game;
        } else {
            throw new AccessDeniedException("You are not part of this game");
        }
    }

    @Transactional(readOnly = true)
    public List<Game> findFinished(){
        return gameRepository.findByEndDateNotNull();
    }

    @Transactional(readOnly = true)
    public List<Game> findPlaying() {
        return gameRepository.findByStartDateNotNullAndEndDateIsNull();
    }

    @Transactional(readOnly = true)
    public List<Game> findPast() {
        return gameRepository.findByEndDateIsNotNull();
    }

    @Transactional
    public void saveGame(Game game) throws DataAccessException {
        gameRepository.save(game);
    }

    @Transactional
    public Game updateGame(@Valid Game game, Integer idToUpdate) {
        Game toUpdate = findById(idToUpdate);
        BeanUtils.copyProperties(game, toUpdate, "id");
        gameRepository.save(toUpdate);

        return toUpdate;
    }

    @Transactional
    public void deleteGame(Game toDelete) {
        gameRepository.delete(toDelete);
    }

    @Transactional(readOnly = true)
    public List<Game> findGamesByPlayerUsername(String username) {
        User currentUser = userService.findCurrentUser();

        if (!currentUser.hasAuthority("ADMIN") && !currentUser.getUsername().equals(username)) {
            throw new IllegalStateException("You cannot see other players' games");
        }

        return gameRepository.findGamesByPlayerUsername(username);
    }

    @Transactional(readOnly = true)
    public List<Game> findGamesByWinnerUsername(String username) {
        return gameRepository.findGamesByWinnerUsername(username);
    }

    @Transactional
    public Game createGame(String password, User user) {
        Game game = new Game();
        game.setPassword((password != null && !password.isBlank()) ? password : null);
        game.setLeader(user);
        Player player = new Player();
        player.setUser(user);
        player.setGame(game);
        playerService.savePlayer(player);
        game.getPlayers().add(player);
        user.getPlayers().add(player);
        return gameRepository.save(game);
    }

    @Transactional
    public Game findByIdForUpdate(Integer id) {
        return gameRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", id));
    }

    @Transactional(rollbackFor = {PlayerAlreadyInGameException.class, TooManyPlayersException.class})
    public Game joinGame(Integer gameId, User user, String password)
        throws PlayerAlreadyInGameException, TooManyPlayersException {
        Game game = findByIdForUpdate(gameId);
        if (game.getPassword() != null && !game.getPassword().isBlank() && !game.getPassword().equals(password)) {
            throw new IllegalArgumentException("Incorrect password");
        }
        addPlayerToGame(game, user);
        return game;
    }

    void ensureLobbyOpen(Game game) {
        if (game.getStartDate() != null || game.hasFinished() || !game.getTurns().isEmpty()) {
            throw new IllegalStateException("The game has already started or finished");
        }
    }

    @Transactional(rollbackFor = {PlayerAlreadyInGameException.class, TooManyPlayersException.class})
    public void addPlayerToGame(Game game, User user) throws PlayerAlreadyInGameException, TooManyPlayersException {
        ensureLobbyOpen(game);
        if (playerInGame(game, user)) {
            throw new PlayerAlreadyInGameException();
        }
        if (game.getPlayers().size() >= 4) {
            throw new TooManyPlayersException();
        }

        Player player = new Player();
        player.setGame(game);
        player.setUser(user);
        playerService.savePlayer(player);
        game.getPlayers().add(player);
        user.getPlayers().add(player);
        gameRepository.save(game);
    }

    @Transactional
    public void removePlayerFromGame(Game game, User user) {
        Player playerToRemove = game.getPlayers().stream()
            .filter(p -> p.getUser().getId().equals(user.getId()))
            .findFirst()
            .orElseThrow();

        game.getPlayers().remove(playerToRemove);
    }

    @Transactional(readOnly = true)
    public boolean playerInGame(Game game, User user) {
        return game.getPlayers().stream()
            .anyMatch(p -> p.getUser().getId().equals(user.getId()));
    }

    @Transactional(readOnly = true)
    public boolean playerSpectating(Game game, User user) {
        return game.getSpectators().stream()
            .anyMatch(u -> u.getId().equals(user.getId()));
    }

    @Transactional
    public void endGameIfNeeded(Game game, Turn turn) {
        if (game.getEndDate() != null) {
            return;
        }

        int maxTurns = game.getPlayers().size() * 2;
        int totalTurnsPlayed = game.getTurns().size();

        int expectedVotes = game.getPlayers().size() - 1;
        int votesThisTurn = turn.getPlayedCards().stream()
            .mapToInt(c -> c.getVotes().size())
            .sum();

        boolean maxTurnsReached = totalTurnsPlayed >= maxTurns;
        boolean allVotesIn = votesThisTurn == expectedVotes;

        if (maxTurnsReached && allVotesIn) {
            game.setEndDate(new Date());
            gameRepository.save(game);
        }
    }

    @Transactional
    public void declareWinners(Game game) {
        List<Player> players = game.getPlayers();
        //get player(s) with max points
        int maxPoints = players.stream()
            .mapToInt(Player::getPoints)
            .max()
            .orElse(0);
        List<Player> winners = players.stream()
            .filter(p -> p.getPoints() == maxPoints)
            .collect(Collectors.toList());
        game.setWinners(winners);
        game.setSpectators(new ArrayList<>());
        gameRepository.save(game);
        game.getWinners().stream().map(Player::getUser).forEach(achievementService::checkWinAchievements);
        game.getPlayers().stream().map(Player::getUser).forEach(u -> {
            achievementService.checkPlayAchievements(u);
            achievementService.checkTrickAchievements(u);
        });
    }

    @Transactional
    public void addSpectatorsToGame(Game game) {
        List<User> usersInGame = game.getPlayers().stream().map(Player::getUser).toList();
        List<User> friendsOfAllPlayers = usersInGame.stream().map(friendRequestService::getFriends).reduce((list1, list2) ->
        {List<User> intersection = new ArrayList<>(list1);intersection.retainAll(list2);
            return intersection;
        }).orElse(new ArrayList<>());
        game.setSpectators(friendsOfAllPlayers);
        gameRepository.save(game);
    }
}

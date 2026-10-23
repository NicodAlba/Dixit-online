package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.exceptions.*;
import es.us.dp1.lx_xy_24_25.dixit.player.*;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.StreamSupport;

/** Projects an independent, locked aggregate snapshot after commands have committed. */
@Service
@Transactional(propagation=Propagation.REQUIRES_NEW)
public class GameViews {
    private final GameRepository games;
    private final TurnRepository turns;
    private final PlayerRepository players;
    private final UserService users;
    private final EntityManager entities;
    public GameViews(GameRepository games,TurnRepository turns,PlayerRepository players,UserService users,EntityManager entities){
        this.games=games;this.turns=turns;this.players=players;this.users=users;this.entities=entities;
    }
    private Game snapshot(Integer id){
        // OSIV can retain entities loaded by a preceding command; don't serialize that cache.
        entities.clear();
        return games.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Game","id",id));
    }
    private GameView project(Game game,Integer userId,Integer requestedPerspective,boolean spectatorOnly){
        Player own=game.getPlayers().stream().filter(p->Objects.equals(p.getUser().getId(),userId)).findFirst().orElse(null);
        boolean spectator=game.getSpectators().stream().anyMatch(u->Objects.equals(u.getId(),userId));
        if((own==null&&!spectator)||(spectatorOnly&&!spectator))throw new AccessDeniedException("You are not authorized to view this game.");
        Integer perspective=own==null?requestedPerspective:own.getId();
        if(own!=null && requestedPerspective!=null && !Objects.equals(own.getId(),requestedPerspective))throw new AccessDeniedException("Participants cannot view a rival's hand.");
        if(perspective!=null && game.getPlayers().stream().noneMatch(p->Objects.equals(p.getId(),perspective)))throw new InvalidRequestException("The selected perspective does not belong to this game.");
        game.getTurns().stream().filter(t->t.getState()!=TurnState.FINISHED && t.getTableOrderSeed()==null).forEach(t->t.setTableOrderSeed(UUID.randomUUID().toString()));
        return GameView.of(game,perspective);
    }
    public GameView game(Integer id,Integer perspective){Integer user=users.findCurrentUser().getId();return project(snapshot(id),user,perspective,false);}
    public GameView spectate(Integer id,Integer perspective){Integer user=users.findCurrentUser().getId();return project(snapshot(id),user,perspective,true);}
    public GameView.Round turn(Integer id,Integer perspective){
        Integer user=users.findCurrentUser().getId();
        Integer gameId=turns.findGameIdById(id).orElseThrow(()->new ResourceNotFoundException("Turn","id",id));
        GameView view=project(snapshot(gameId),user,perspective,false);
        return view.turns().stream().filter(t->Objects.equals(t.id(),id)).findFirst().orElseThrow(()->new ResourceNotFoundException("Turn","id",id));
    }
    public GameView.Participant player(Integer id,Integer perspective){
        User actor=users.findCurrentUser();Integer userId=actor.getId();
        Player player=players.findById(id).orElseThrow(()->new ResourceNotFoundException("Player","id",id));
        if(player.getGame()==null){
            if(!Objects.equals(player.getUser().getId(),userId))throw new AccessDeniedException();
            return GameView.participant(player,player.getId());
        }
        Integer gameId=player.getGame().getId();
        return project(snapshot(gameId),userId,perspective,false).players().stream().filter(p->Objects.equals(p.id(),id)).findFirst().orElseThrow();
    }
    public List<GameView.PlayedCard> cards(Integer id,Integer perspective){
        User actor=users.findCurrentUser();Integer userId=actor.getId();
        Player player=players.findById(id).orElseThrow(()->new ResourceNotFoundException("Player","id",id));
        if(player.getGame()==null){
            if(!Objects.equals(player.getUser().getId(),userId))throw new AccessDeniedException();
            return GameView.participant(player,player.getId()).cards();
        }
        GameView view=project(snapshot(player.getGame().getId()),userId,perspective,false);
        if(!Objects.equals(view.perspectivePlayerId(),id))throw new AccessDeniedException("This hand is private.");
        return view.players().stream().filter(p->Objects.equals(p.id(),id)).findFirst().orElseThrow().cards();
    }
    public List<GameView.Catalog> catalog(String kind){
        if(!users.findCurrentUser().hasAuthority("ADMIN"))throw new AccessDeniedException();
        Iterable<Game> found=switch(kind){case "playing"->games.findByStartDateNotNullAndEndDateIsNull();case "past"->games.findByEndDateIsNotNull();default->games.findAll();};
        return StreamSupport.stream(found.spliterator(),false).map(GameView::catalog).toList();
    }
    public List<GameView.Catalog> history(String username){
        User actor=users.findCurrentUser();String target=username==null?actor.getUsername():username;
        if(!actor.hasAuthority("ADMIN")&&!Objects.equals(actor.getUsername(),target))throw new AccessDeniedException();
        return games.findGamesByPlayerUsername(target).stream().map(GameView::catalog).toList();
    }
}

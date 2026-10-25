package es.us.dp1.lx_xy_24_25.dixit.game;

import com.fasterxml.jackson.annotation.JsonInclude;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable wire views: no entity graph, stock, credentials or rival hands. */
public record GameView(Integer id, Date startDate, Date endDate, boolean cancelled, Account leader, boolean hasPassword,
                       Integer perspectivePlayerId, List<Participant> players, List<Round> turns, List<Person> winners) {
    public record Account(Integer id,String username) {}
    public record Person(Integer id,Account user,Integer points) {}
    public record Image(Integer id) {}
    public record Participant(Integer id,Account user,Integer points,int handSize,List<PlayedCard> cards) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PlayedCard(Integer id,Image image,boolean isOwn,Person owner,Account user,List<Person> votes) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Round(Integer id,TurnState state,String clue,Person storyteller,PlayedCard answer,
                        List<PlayedCard> playedCards,int playedCardCount,boolean hasPlayed,boolean hasVoted,Integer ownVotedCardId) {}
    public record Catalog(Integer id,Date startDate,Date endDate,boolean cancelled,Account leader,boolean hasPassword,
                          List<Person> players,List<Person> winners) {}

    static Account account(User user){return user==null?null:new Account(user.getId(),user.getUsername());}
    static Person person(Player player){return player==null?null:new Person(player.getId(),account(player.getUser()),player.getPoints());}
    static List<Card> hand(Player player){return player.getCards().stream().filter(c->c.getTurn()==null && c.getOwner()!=null && Objects.equals(c.getOwner().getId(),player.getId())).toList();}
    static Participant participant(Player player,Integer perspective){
        List<Card> hand=hand(player);
        return new Participant(player.getId(),account(player.getUser()),player.getPoints(),hand.size(),
            Objects.equals(player.getId(),perspective)?hand.stream().map(c->card(c,player,false)).toList():List.of());
    }
    static PlayedCard card(Card card,Player perspective,boolean revealed){
        if(card==null)return null;
        boolean own=perspective!=null && ((card.getOwner()!=null && Objects.equals(card.getOwner().getId(),perspective.getId()))
            || (card.getUser()!=null && Objects.equals(card.getUser().getId(),perspective.getUser().getId())));
        return new PlayedCard(card.getId(),card.getImage()==null?null:new Image(card.getImage().getId()),own,
            revealed?person(card.getOwner()):null,revealed?account(card.getUser()):null,
            revealed?card.getVotes().stream().map(GameView::person).toList():null);
    }
    static Round round(Turn turn,Player perspective){
        boolean revealed=turn.getState()==TurnState.FINISHED;
        boolean tableVisible=revealed || turn.getState()==TurnState.SHOWING_CARDS || turn.getState()==TurnState.VOTING;
        boolean storyteller=perspective!=null && turn.getStoryteller()!=null && Objects.equals(perspective.getId(),turn.getStoryteller().getId());
        boolean hasPlayed=perspective!=null && turn.getPlayedCards().stream().anyMatch(c->card(c,perspective,false).isOwn());
        Integer ownVotedCardId=perspective==null?null:turn.getPlayedCards().stream()
            .filter(c->c.getVotes().stream().anyMatch(p->Objects.equals(p.getId(),perspective.getId())))
            .map(Card::getId).findFirst().orElse(null);
        List<Card> visible=turn.getPlayedCards().stream().filter(c->tableVisible || (perspective!=null && card(c,perspective,false).isOwn())).toList();
        List<PlayedCard> table=visible.stream().sorted(Comparator.comparing(c->order(turn,c))).map(c->card(c,perspective,revealed)).toList();
        return new Round(turn.getId(),turn.getState(),turn.getClue(),person(turn.getStoryteller()),
            revealed || storyteller?card(turn.getAnswer(),perspective,revealed):null,table,turn.getPlayedCards().size(),hasPlayed,ownVotedCardId!=null,ownVotedCardId);
    }
    private static String order(Turn turn,Card card){
        // Legacy finished rounds need no secret and are never rewritten just for serialization.
        if(turn.getTableOrderSeed()==null)return String.format("%010d",card.getId());
        try{
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(turn.getTableOrderSeed().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(String.valueOf(card.getId()).getBytes(StandardCharsets.UTF_8)));
        }catch(java.security.GeneralSecurityException impossible){throw new IllegalStateException("Cannot order the table.",impossible);}
    }
    static GameView of(Game game,Integer perspective){
        Player viewer=game.getPlayers().stream().filter(p->Objects.equals(p.getId(),perspective)).findFirst().orElse(null);
        return new GameView(game.getId(),game.getStartDate(),game.getEndDate(),game.isCancelled(),account(game.getLeader()),game.getPassword()!=null&&!game.getPassword().isBlank(),
            perspective,game.getPlayers().stream().map(p->participant(p,perspective)).toList(),
            game.getTurns().stream().sorted(Comparator.comparing(Turn::getId)).map(t->round(t,viewer)).toList(),game.getWinners().stream().map(GameView::person).toList());
    }
    static Catalog catalog(Game game){return new Catalog(game.getId(),game.getStartDate(),game.getEndDate(),game.isCancelled(),account(game.getLeader()),game.getPassword()!=null&&!game.getPassword().isBlank(),
        game.getPlayers().stream().map(GameView::person).toList(),game.getWinners().stream().map(GameView::person).toList());}
}

package es.us.dp1.lx_xy_24_25.dixit.game;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Shared notifications contain identifiers only. Actor views are fetched over authenticated REST. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameEvent(String type,Integer gameId,Integer turnId) {
    public static GameEvent changed(Integer id){return new GameEvent("GAME_UPDATED",id,null);}
    public static GameEvent turn(Integer gameId,Integer turnId){return new GameEvent("TURN_UPDATED",gameId,turnId);}
    public static GameEvent deleted(Integer id){return new GameEvent("DELETED",id,null);}
}

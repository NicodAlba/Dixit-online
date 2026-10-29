import {act, renderHook} from "@testing-library/react";
import useGameMotion from "./useGameMotion";
const snapshot = (turnId=10,state="WAITING_PLAYER_CARDS",cards=[1,2],count=1) => ({id:42, players:[{id:70,cards:cards.map(id=>({id}))}], turns:[{id:turnId,state,playedCardCount:count}]});
beforeEach(()=>jest.useFakeTimers());afterEach(()=>jest.useRealTimers());
test("initial and identical snapshots are silent; accepted aggregate plays never carry rival identities",()=>{
 const {result,rerender}=renderHook(props=>useGameMotion(props),{initialProps:{game:snapshot(),connection:"connected",perspectiveId:70}});
 expect(result.current).toBeNull();
 rerender({game:snapshot(10,"WAITING_PLAYER_CARDS",[1,2],2),connection:"connected",perspectiveId:70});
 expect(result.current).toMatchObject({kind:"card-played",arrivingCardIds:[]});
 expect(Object.keys(result.current).sort()).toEqual(["arrivingCardIds","key","kind"]);
 const key=result.current.key;
 rerender({game:snapshot(10,"WAITING_PLAYER_CARDS",[1,2],2),connection:"connected",perspectiveId:70});expect(result.current.key).toBe(key);
 act(()=>jest.advanceTimersByTime(1400));expect(result.current).toBeNull();
});
test("new rounds animate only new Card IDs, retaining reused Image appearances and suppressing reconnect history",()=>{
 const {result,rerender}=renderHook(props=>useGameMotion(props),{initialProps:{game:snapshot(),connection:"connected",perspectiveId:70}});
 rerender({game:snapshot(11,"WAITING_STORYTELLER_CARD",[2,101]),connection:"connected",perspectiveId:70});
 expect(result.current).toMatchObject({kind:"new-round",arrivingCardIds:[101]});
 rerender({game:snapshot(11),connection:"reconnecting",perspectiveId:70});expect(result.current).toBeNull();
 rerender({game:snapshot(12,"SHOWING_CARDS",[200,201]),connection:"connected",perspectiveId:70});expect(result.current).toBeNull();
});
test("latest reveal replaces a previous effect without reordering cards; perspective and cancellation clear effects",()=>{
 const {result,rerender}=renderHook(props=>useGameMotion(props),{initialProps:{game:snapshot(),connection:"connected",perspectiveId:70}});
 const game=snapshot(10,"SHOWING_CARDS");game.turns[0].playedCards=[{id:9},{id:4},{id:8}];
 rerender({game,connection:"connected",perspectiveId:70});expect(result.current.kind).toBe("reveal");expect(game.turns[0].playedCards.map(c=>c.id)).toEqual([9,4,8]);
 rerender({game,connection:"connected",perspectiveId:71});expect(result.current).toBeNull();
 rerender({game:{...snapshot(11),cancelled:true},connection:"connected",perspectiveId:71});expect(result.current).toBeNull();
});
test("completion never deals cards and unmount clears effect timers",()=>{
 const {result,rerender,unmount}=renderHook(props=>useGameMotion(props),{initialProps:{game:snapshot(),connection:"connected",perspectiveId:70}});
 rerender({game:snapshot(11),connection:"connected",perspectiveId:70});expect(result.current.kind).toBe("new-round");
 rerender({game:{...snapshot(12,"FINISHED",[100,101]),endDate:"finished"},connection:"connected",perspectiveId:70});expect(result.current).toBeNull();
 unmount();expect(jest.getTimerCount()).toBe(0);
});

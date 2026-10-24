import React from 'react';
import {act,render,screen,waitFor} from '@testing-library/react';
import {Client} from '@stomp/stompjs';
import useGameTurnState from './useGameTurnState';
jest.mock('@stomp/stompjs',()=>({Client:jest.fn()}));
jest.mock('sockjs-client',()=>jest.fn());
const showMessage=jest.fn(),showVisible=jest.fn();
const first={id:42,players:[{id:1,cards:[{id:100,image:{id:7}}],handSize:6}],turns:[{id:31,state:'WAITING_STORYTELLER_CARD'}],winners:[]};
const next={...first,turns:[{id:31,state:'FINISHED'},{id:32,state:'WAITING_STORYTELLER_CARD'}]};
let client,callbacks;
function StateHarness({token='token',perspective=null}={}){
 const state=useGameTurnState({gameId:42,jwt:token,perspectivePlayerId:perspective,setMessage:showMessage,setVisible:showVisible});
 return <div data-testid="state">{JSON.stringify({game:state.game,turnId:state.turnId,loading:state.loading,connection:state.connection})}</div>;
}
const reply=data=>({ok:true,json:async()=>data});
const originalFetch=global.fetch;
beforeEach(()=>{
 jest.clearAllMocks();callbacks={};
 client={connected:true,activate:jest.fn(),deactivate:jest.fn(),subscribe:jest.fn((destination,callback)=>{callbacks[destination]=callback;return {unsubscribe:jest.fn()};})};
 Client.mockImplementation(()=>client);global.fetch=jest.fn().mockResolvedValue(reply(first));
});
afterEach(()=>{global.fetch=originalFetch;});
const current=()=>JSON.parse(screen.getByTestId('state').textContent);
test('loads one coherent actor snapshot and authenticates the connection',async()=>{
 render(React.createElement(StateHarness));await waitFor(()=>expect(current().turnId).toBe(31));
 expect(fetch).toHaveBeenCalledTimes(1);expect(fetch.mock.calls[0][0]).toBe('/api/v1/games/42');
 expect(Client.mock.calls[0][0].connectHeaders).toEqual({Authorization:'Bearer token'});
});
test('shared payload cannot install a foreign hand; an invalidation reloads REST',async()=>{
 render(React.createElement(StateHarness));await waitFor(()=>expect(current().turnId).toBe(31));
 await act(async()=>client.onConnect());
 global.fetch.mockResolvedValue(reply(next));
 await act(async()=>callbacks['/topic/game/42']({body:JSON.stringify({type:'GAME_UPDATED',gameId:42,players:[{cards:[{image:{id:999}}]}]})}));
 await waitFor(()=>expect(current().turnId).toBe(32));
 expect(current().game.players[0].cards[0].image.id).toBe(7);
 expect(client.subscribe).toHaveBeenCalledWith('/topic/turn/32',expect.any(Function));
});
test('reconnect reloads missed state without replaying actions',async()=>{
 render(React.createElement(StateHarness));await waitFor(()=>expect(current().turnId).toBe(31));
 await act(async()=>client.onConnect());act(()=>client.onWebSocketClose());
 global.fetch.mockResolvedValue(reply(next));
 await act(async()=>client.onConnect());await waitFor(()=>expect(current().turnId).toBe(32));
 expect(fetch.mock.calls.every(([,options])=>!options.method||options.method==='GET')).toBe(true);
});
test('older response cannot replace a snapshot recovered after reconnect',async()=>{
 let complete;global.fetch.mockReturnValueOnce(new Promise(resolve=>{complete=resolve;})).mockResolvedValue(reply(next));
 render(React.createElement(StateHarness));await act(async()=>client.onConnect());
 await waitFor(()=>expect(current().turnId).toBe(32));
 await act(async()=>complete(reply(first)));expect(current().turnId).toBe(32);
});
test('spectator perspective is explicit in the private REST request',async()=>{
 render(React.createElement(StateHarness,{perspective:17}));await waitFor(()=>expect(current().turnId).toBe(31));
 expect(fetch.mock.calls[0][0]).toBe('/api/v1/games/42?perspectivePlayerId=17');
});
test('no session makes no private request or connection',()=>{
 render(React.createElement(StateHarness,{token:null}));expect(fetch).not.toHaveBeenCalled();expect(Client).not.toHaveBeenCalled();
});
test('deletion clears locally held cards and turn state',async()=>{
 render(React.createElement(StateHarness));await waitFor(()=>expect(current().turnId).toBe(31));
 await act(async()=>client.onConnect());
 act(()=>callbacks['/topic/game/42']({body:JSON.stringify({type:'DELETED',gameId:42})}));
 expect(current().game.deleted).toBe(true);expect(current().game.players).toEqual([]);expect(current().turnId).toBeNull();
});
test('real permission and network failures retain feedback',async()=>{
 global.fetch.mockResolvedValue({ok:false,status:403,json:async()=>({message:'Not a participant'})});
 render(React.createElement(StateHarness));await waitFor(()=>expect(showMessage).toHaveBeenCalledWith('Not a participant'));
 expect(showVisible).toHaveBeenCalledWith(true);
});

test('reconnecting remains visible until the recovered authorized view has loaded', async () => {
 render(React.createElement(StateHarness));
 await waitFor(()=>expect(current().loading).toBe(false));
 await act(async()=>client.onConnect());
 expect(current().connection).toBe('connected');
 act(()=>{client.connected=false; client.onWebSocketClose();});
 expect(current().connection).toBe('reconnecting');
 let finish;
 global.fetch.mockReturnValueOnce(new Promise(resolve=>{finish=resolve;}));
 act(()=>{client.connected=true; client.onConnect();});
 expect(current().connection).toBe('refreshing');
 await act(async()=>finish(reply(next)));
 expect(current().connection).toBe('connected');
 expect(current().turnId).toBe(32);
 expect(fetch.mock.calls.every(([,options])=>!options.method||options.method==='GET')).toBe(true);
});

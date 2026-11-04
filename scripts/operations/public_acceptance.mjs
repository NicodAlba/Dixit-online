/** Owner-run HTTP/STOMP acceptance against the approved public demo; never CI. */
import {createRequire} from 'node:module';
import {randomUUID} from 'node:crypto';
import {writeFile} from 'node:fs/promises';
import {parseArgs} from 'node:util';
import {createInterface} from 'node:readline/promises';
import assert from 'node:assert/strict';
const require=createRequire(new URL('../../frontend/package.json',import.meta.url));
const WebSocket=require('ws'); // Existing locked frontend tooling; npm ci first.
const {values}=parseArgs({options:{url:{type:'string'},'private-output':{type:'string'},'acknowledge-fixtures':{type:'boolean'},'visual-pauses':{type:'boolean'}}});
assert(!process.env.CI && !process.env.GITHUB_ACTIONS,'Not an ordinary CI task');
assert(values['acknowledge-fixtures'] && values['private-output'],'Explicit fixture acknowledgement and private output required');
const base=new URL(values.url);assert(base.protocol==='https:' && base.hostname.endsWith('.onrender.com') && base.pathname==='/','Approved HTTPS Render demo only');
const origin=base.origin;
async function api(method,path,actor,body,expected=200){
 const response=await fetch(origin+'/api/v1'+path,{method,headers:{'Content-Type':'application/json',...(actor?{Authorization:'Bearer '+actor.token}:{})},...(body===undefined?{}:{body:typeof body==='string'?body:JSON.stringify(body)}),signal:AbortSignal.timeout(60000)});
 assert.equal(response.status,expected,method+' '+path+' status');
 const text=await response.text();return text?JSON.parse(text):null;
}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
class Stomp {
 constructor(actor){this.actor=actor;this.frames=[];this.events=[];this.buffer='';}
 async connect(){
  this.ws=new WebSocket(origin.replace('https:','wss:')+'/ws/websocket',{origin,handshakeTimeout:30000});
  this.ws.on('message',bytes=>{this.buffer+=bytes.toString();let end;while((end=this.buffer.indexOf('\0'))!==-1){const frame=this.buffer.slice(0,end).trimStart();this.buffer=this.buffer.slice(end+1);this.frames.push(frame);if(frame.startsWith('MESSAGE')){const event=JSON.parse(frame.split('\n\n').slice(1).join('\n\n'));assert(Object.keys(event).every(k=>['type','gameId','turnId'].includes(k)),'Private STOMP payload');this.events.push(event);}}});
  await new Promise((resolve,reject)=>{this.ws.once('open',resolve);this.ws.once('error',()=>reject(new Error('WebSocket handshake failed')));});
  this.send('CONNECT',{ 'accept-version':'1.2',host:base.hostname,'heart-beat':'10000,10000',Authorization:'Bearer '+this.actor.token});
  await this.until(()=>this.frames.some(f=>f.startsWith('CONNECTED')));
  this.heartbeat=setInterval(()=>{if(this.ws.readyState===WebSocket.OPEN)this.ws.send('\n');},10000);return this;
 }
 send(command,headers){this.ws.send(command+'\n'+Object.entries(headers).map(([k,v])=>k+':'+v).join('\n')+'\n\n\0');}
 async until(test){const deadline=Date.now()+30000;while(!test()){assert(Date.now()<deadline,'STOMP response timeout');assert(!this.frames.some(f=>f.startsWith('ERROR')),'STOMP rejected request');await sleep(50);}}
 async subscribe(game){const count=this.events.length;this.send('SUBSCRIBE',{id:'game',destination:'/topic/game/'+game,ack:'auto'});await this.until(()=>this.events.length>count);}
 close(){clearInterval(this.heartbeat);this.ws.close();}
}
const terminal=values['visual-pauses']?createInterface({input:process.stdin,output:process.stdout}):null;
const visual=async stage=>{if(terminal)await terminal.question(stage+' — inspect browser, then Enter: ');};
const accounts=[],connections=[];
try {
 for(let i=0;i<4;i++){
  const username='demo_'+randomUUID().replaceAll('-','').slice(0,20)+'_'+i,password=randomUUID()+randomUUID();
  await api('POST','/auth/signup',null,{username,password,authority:'PLAYER'},201);
  const account=await api('POST','/auth/signin',null,{username,password});accounts.push({...account,username,password});
 }
 await writeFile(values['private-output'],JSON.stringify({origin,accounts},null,2),{mode:0o600,flag:'wx'});
 const host=accounts[0],guest=accounts[1];
 await api('POST','/friends/addFriend/'+guest.id,host);
 const relationship=(await api('GET','/friends/relationships',guest)).find(r=>r.accountId===host.id);assert(relationship?.requestId);
 await api('PATCH','/friends/'+relationship.requestId+'/accept',guest);
 assert((await api('GET','/friends',host)).some(friend=>friend.id===guest.id||friend.accountId===guest.id),'Accepted friend missing');
 const created=await api('POST','/games',host,{},201),game=created.id;
 await api('POST',`/games/${game}/invite`,host,{invited:String(guest.id)});
 const invitation=(await api('GET','/games/invitations',guest)).find(i=>i.gameId===game);assert(invitation);
 await api('PATCH','/games/invitations/'+invitation.id+'/accept',guest);
 for(const account of accounts.slice(2))await api('PATCH',`/games/${game}/join`,account,{});
 for(const account of accounts){const connection=await new Stomp(account).connect();connections.push(connection);await connection.subscribe(game);}
 await visual('Four-player lobby '+game);
 await api('PATCH',`/games/${game}/start`,host);
 let last=null;const narrators=new Map(),hands=new Map();
 for(let round=0;round<8;round++){
  const view=await api('GET','/games/'+game,host),turn=view.turns.at(-1),route='/turns/'+turn.id;
  assert(!view.cancelled && !view.endDate,'Game ended early');
  const storyteller=accounts.find(a=>a.id===turn.storyteller.user.id);assert(storyteller);
  narrators.set(storyteller.id,(narrators.get(storyteller.id)||0)+1);
  const own=new Map();
  for(const actor of accounts){
   const state=await api('GET','/games/'+game,actor);assert.equal(state.players.length,4);
   for(const player of state.players){if(player.user.id===actor.id){own.set(actor.id,player);assert.equal(player.cards.length,6);const previous=hands.get(actor.id)||new Set();player.cards.forEach(c=>previous.add(c.id));hands.set(actor.id,previous);}else assert.equal(player.cards.length,0,'Rival hand leaked');}
  }
  if(round===0)await visual('Initial hand and storyteller selection');
  const narrator=own.get(storyteller.id),answer=narrator.cards[0].id;
  await api('PUT',route+'/answer/'+answer,storyteller);
  await api('PUT',route+'/clue',storyteller,'Bright paths beyond the clouds');
  if(round===0)await visual('Clue submitted; participants choose cards');
  if(round===0){connections[1].close();await sleep(1000);connections[1]=await new Stomp(accounts[1]).connect();await connections[1].subscribe(game);assert(!(await api('GET','/games/'+game,host)).cancelled,'Reconnect cancelled game');}
  for(const actor of accounts)if(actor.id!==storyteller.id){const player=own.get(actor.id);await api('POST',route+`/play-card/${player.cards[0].id}/${player.id}`,actor);}
  if(round===0)await visual('Anonymous table before votes');
  let order;
  for(const actor of accounts)if(actor.id!==storyteller.id){
   const state=await api('GET',route,actor);assert(!state.answer,'Answer leaked before reveal');assert.equal(state.playedCards.length,4);
   for(const card of state.playedCards)assert(!('owner' in card)&&!('user' in card)&&!('votes' in card),'Authorship/votes leaked');
   const ids=state.playedCards.map(c=>c.id);if(order)assert.deepEqual(ids,order,'Anonymous order differs');else order=ids;
  }
  for(const actor of accounts)if(actor.id!==storyteller.id)await api('POST',route+`/vote/${own.get(actor.id).id}/${answer}`,actor);
  last=await api('GET','/games/'+game,host);
  console.log('Completed round',round+1,'with actor-specific REST views and live STOMP sessions');
 }
 assert(last.endDate&&!last.cancelled);assert.equal(last.turns.length,8);assert(last.turns.every(t=>t.state==='FINISHED'));
 assert([...narrators.values()].every(n=>n===2));assert([...hands.values()].every(cards=>cards.size>6),'No replenishment seen');
 for(const actor of accounts){const stats=await api('GET','/stats/user/'+actor.id,actor);assert.equal(stats.gamesPlayed,1);await api('GET','/users/'+actor.id+'/achievements',actor);const history=await api('GET','/games/my',actor);assert(history.some(g=>g.id===game));}
 assert(connections.every(c=>c.events.length>1),'Missing live invalidations');
 await writeFile(values['private-output']+'.result.json',JSON.stringify({origin,gameId:game,rounds:8,participants:4,completed:true,reconnected:true,stompEvents:connections.map(c=>c.events.length),checkedAt:new Date().toISOString()},null,2),{mode:0o600,flag:'wx'});
 await visual('Completed game '+game+' and history');
 console.log('PASS: four-player HTTP/STOMP acceptance, reconnect, privacy, replenishment, history/statistics/achievements. This is not visual acceptance.');
} finally {connections.forEach(c=>c.close());terminal?.close();}

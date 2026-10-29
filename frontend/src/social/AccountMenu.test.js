import React from "react";
import {act,fireEvent,render,screen,waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {SocialProvider} from "./SocialContext";
import AccountMenu from "./AccountMenu";
import OpponentPanel from "../game/components/OpponentPanel";
import Chat from "../game/components/Chat";
jest.mock("../services/token.service",()=>({getUser:()=>({id:71,username:"Same name"}),getLocalAccessToken:()=>"token"}));
jest.mock("../hooks/useChatMessages",()=>()=>({messages:[{accountId:72,username:"Same name",text:"Hello"}],loading:false,sending:false,error:null,send:jest.fn()}));
const originalFetch=global.fetch;
const ok=data=>({ok:true,json:async()=>data});
let relationships;
beforeEach(()=>{
 relationships=[];
 global.fetch=jest.fn(async(url,options)=>{
  if(url==="/api/v1/friends" || url==="/api/v1/games/invitations")return ok([]);
  if(url==="/api/v1/friends/relationships")return ok(relationships);
  throw new Error(`Unexpected request ${url}`);
 });
});
afterEach(()=>{global.fetch=originalFetch;});
const wrap=children=><MemoryRouter><SocialProvider jwt="token">{children}</SocialProvider></MemoryRouter>;
test("participant actions use User.id, guard duplicate clicks and recover outgoing state from the server",async()=>{
 let finish;
 global.fetch.mockImplementation(async(url,options)=>{
  if(url==="/api/v1/friends" || url==="/api/v1/games/invitations")return ok([]);
  if(url==="/api/v1/friends/relationships")return ok(relationships);
  if(url==="/api/v1/friends/addFriend/72")return new Promise(resolve=>{finish=resolve;});
  throw new Error(`Unexpected request ${url}`);
 });
 const player={id:901,user:{id:72,username:"Owl"},points:0,handSize:6};
 const view=()=>wrap(<OpponentPanel position="left" player={player} storyteller={{id:903}} getPlayerName={p=>p.user.username}/>);
 const first=render(view());await act(async()=>{});
 expect(global.fetch.mock.calls.every(([url, options])=>options.method === "GET" && ["/api/v1/friends", "/api/v1/friends/relationships", "/api/v1/games/invitations"].includes(url))).toBe(true);
 fireEvent.click(screen.getByRole("button",{name:"Owl: player options"}));
 const add=await screen.findByRole("button",{name:"Add friend"});await waitFor(()=>expect(add).toBeEnabled());
 add.focus();fireEvent.click(add);fireEvent.click(add);
 expect(screen.getByRole("button",{name:"Close player options"})).toHaveFocus();
 expect(global.fetch.mock.calls.filter(([,o])=>o.method==="POST")).toHaveLength(1);
 expect(global.fetch).toHaveBeenCalledWith("/api/v1/friends/addFriend/72",expect.objectContaining({method:"POST"}));
 relationships=[{accountId:72,username:"Owl",status:"OUTGOING",requestId:410}];
 await act(async()=>finish(ok({})));
 expect(await screen.findByRole("button",{name:"Request sent"})).toBeDisabled();
 first.unmount();render(view());fireEvent.click(screen.getByRole("button",{name:"Owl: player options"}));
 expect(await screen.findByRole("button",{name:"Request sent"})).toBeDisabled();
});
test("incoming request is accepted by its request ID and an accepted friend cannot be requested again",async()=>{
 relationships=[{accountId:72,status:"INCOMING",requestId:417}];
 const base=global.fetch;
 global.fetch=jest.fn(async(url,options)=>{
  if(url==="/api/v1/friends/417/accept"){relationships=[{accountId:72,status:"FRIEND",requestId:417}];return ok({});}
  return base(url,options);
 });
 render(wrap(<AccountMenu accountId={72} username="Owl"/>));fireEvent.click(screen.getByRole("button",{name:"Owl: player options"}));
 const accept=await screen.findByRole("button",{name:"Accept request"});await waitFor(()=>expect(accept).toBeEnabled());fireEvent.click(accept);
 expect(await screen.findByRole("button",{name:"Already friends"})).toBeDisabled();
 expect(global.fetch).toHaveBeenCalledWith("/api/v1/friends/417/accept",expect.objectContaining({method:"PATCH"}));
});
test("self and missing identity trigger no friendship action beyond authorized background reads",async()=>{
 render(wrap(<><AccountMenu accountId={71} username="You"/><AccountMenu username="Legacy"/></>));
 await act(async()=>{});
 expect(screen.queryByRole("button",{name:/player options/})).not.toBeInTheDocument();
 expect(global.fetch.mock.calls.every(([url,options])=>options.method === "GET" && ["/api/v1/friends", "/api/v1/friends/relationships", "/api/v1/games/invitations"].includes(url))).toBe(true);
});
test("chat opens the same action using the author's account ID even when its name equals the reader's",async()=>{
 render(wrap(<Chat gameId={42}/>));
 const trigger=screen.getByRole("button",{name:"Same name: player options"});trigger.focus();fireEvent.click(trigger);
 expect(await screen.findByRole("dialog",{name:"Same name"})).toBeInTheDocument();
 expect(await screen.findByRole("button",{name:"Add friend"})).toBeEnabled();
 expect(screen.getByText("Hello").closest(".chat-message")).toHaveClass("other-message");
 fireEvent.keyUp(screen.getByRole("dialog"),{key:"Escape",keyCode:27});
 await waitFor(()=>expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
 expect(screen.getByRole("button",{name:"Same name: player options"})).toHaveFocus();
});
test("a request rejection keeps useful feedback and permits a deliberate retry",async()=>{
 const base=global.fetch;
 global.fetch=jest.fn((url,options)=>url.includes("addFriend")?Promise.resolve({ok:false,status:400,json:async()=>({message:"Request unavailable. Try again."})}):base(url,options));
 render(wrap(<AccountMenu accountId={72} username="Owl"/>));fireEvent.click(screen.getByRole("button",{name:"Owl: player options"}));
 const add=await screen.findByRole("button",{name:"Add friend"});await waitFor(()=>expect(add).toBeEnabled());fireEvent.click(add);
 expect(await screen.findByRole("alert")).toHaveTextContent("Request unavailable. Try again.");
 fireEvent.click(screen.getByRole("button",{name:"Retry"}));await waitFor(()=>expect(add).toBeEnabled());
});

import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import Chat from "./Chat";
let mockConnection;
const mockNotify = jest.fn(), mockClear = jest.fn();
jest.mock("../../social/SocialContext",()=>({useSocial:()=>({notifyChat:mockNotify,clearChatNotifications:mockClear})}));
jest.mock("../../presence/PresenceContext",()=>({useApplicationConnection:()=>mockConnection}));
jest.mock("../../social/AccountMenu",()=>({__esModule:true,default:({username})=><strong>{username}</strong>}));
jest.mock("../../services/token.service",()=>({getLocalAccessToken:()=>"token",getUser:()=>({id:1})}));
const originalFetch=global.fetch;
let messages;
const message=(id,accountId=2)=>({id,accountId,username:"Alex",text:"Hello"});
async function settle(){await act(async()=>{});}
async function poll(){await act(async()=>{jest.advanceTimersByTime(3000);});}
beforeEach(()=>{jest.useFakeTimers();messages=[];mockConnection={state:"connected",generation:1};mockNotify.mockClear();mockClear.mockClear();global.fetch=jest.fn(async()=>({ok:true,json:async()=>messages}));});
afterEach(()=>{jest.useRealTimers();global.fetch=originalFetch;});
test("history is silent; identical new texts with distinct IDs notify once each, excluding own messages",async()=>{
 messages=[message(10)];render(<Chat gameId={42}/>);await settle();expect(mockNotify).not.toHaveBeenCalled();
 messages=[...messages,message(11),message(12),message(13,1)];await poll();
 expect(mockNotify).toHaveBeenCalledTimes(1);expect(mockNotify.mock.calls[0][1].map(m=>m.id)).toEqual([11,12]);
 await poll();expect(mockNotify).toHaveBeenCalledTimes(1);
});
test("View opens a collapsed chat and focuses its field only after explicit activation",async()=>{
 render(<Chat gameId={42}/>);await settle();const details=screen.getByText("Chat",{exact:false}).closest("details");
 details.open=false;fireEvent(details,new Event("toggle"));await settle();
 messages=[message(1)];await poll();expect(details.open).toBe(false);
 act(()=>mockNotify.mock.calls[0][2]());expect(details.open).toBe(true);expect(screen.getByRole("textbox",{name:"Chat message"})).toHaveFocus();
});
test("reconnection and failed reads do not announce missed history or duplicate an old message",async()=>{
 const view=render(<Chat gameId={42}/>);await settle();
 mockConnection={state:"reconnecting",generation:1};view.rerender(<Chat gameId={42}/>);await settle();
 expect(screen.getByRole("button",{name:"Send"})).toBeDisabled();
 messages=[message(20)];mockConnection={state:"connected",generation:2};view.rerender(<Chat gameId={42}/>);await settle();expect(mockNotify).not.toHaveBeenCalled();
 global.fetch.mockRejectedValueOnce(new Error("Offline"));await poll();messages=[...messages,message(21)];await poll();expect(mockNotify).not.toHaveBeenCalled();
 messages=[...messages,message(22)];await poll();expect(mockNotify.mock.calls[0][1].map(m=>m.id)).toEqual([22]);
});
test("game change clears notices and delayed old reads cannot notify in the new game",async()=>{
 const view=render(<Chat gameId={42}/>);await settle();let finish;
 global.fetch.mockImplementationOnce(()=>new Promise(resolve=>{finish=resolve;}));await poll();
 view.rerender(<Chat gameId={43}/>);await settle();
 await act(async()=>finish({ok:true,json:async()=>[message(90)]}));
 expect(mockClear).toHaveBeenCalledWith(42);expect(mockNotify).not.toHaveBeenCalled();
 view.unmount();expect(mockClear).toHaveBeenCalledWith(43);
});
test("already announced messages cannot be republished under another game's identity",async()=>{
 const view=render(<Chat gameId={42}/>);await settle();messages=[message(50)];await poll();expect(mockNotify).toHaveBeenCalledTimes(1);
 view.rerender(<Chat gameId={43}/>);await settle();
 expect(mockNotify).toHaveBeenCalledTimes(1);expect(mockNotify.mock.calls[0][0]).toBe(42);
});

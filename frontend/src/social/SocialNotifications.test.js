import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {SocialProvider, useSocial} from "./SocialContext";
import SocialToggle from "./SocialToggle";
let mockConnection;
jest.mock("../presence/PresenceContext", () => ({useApplicationConnection: () => mockConnection, usePresenceData: () => ({connections:null})}));
const originalFetch = global.fetch;
let relationships, invitations;
function app(jwt="token") {return <MemoryRouter><SocialProvider key={jwt} jwt={jwt}><SocialToggle /></SocialProvider></MemoryRouter>;}
async function settle() {await act(async()=>{});}
async function tick() {await act(async()=>{jest.advanceTimersByTime(10000);});}
beforeEach(()=>{
    jest.useFakeTimers(); mockConnection={state:"connected",generation:1}; relationships=[];invitations=[];
    global.fetch=jest.fn(async url=>({ok:true,json:async()=>url.endsWith("relationships")?relationships:url.endsWith("invitations")?invitations:[]}));
});
afterEach(()=>{jest.useRealTimers();global.fetch=originalFetch;});
test("initial history is silent; new requests notify once with panel closed without stealing focus",async()=>{
    relationships=[{accountId:2,requestId:20,status:"INCOMING",username:"Past"}];
    render(app());await settle();
    const trigger=screen.getByRole("button",{name:"Friends",exact:true});trigger.focus();
    expect(screen.queryByRole("region",{name:"Social notifications"})).not.toBeInTheDocument();
    relationships=[...relationships,{accountId:3,requestId:30,status:"INCOMING",username:"Alex"}];
    await tick();expect(screen.getByText("Alex sent you a friend request.")).toBeInTheDocument();expect(trigger).toHaveFocus();
    fireEvent.click(screen.getByRole("button",{name:"Dismiss: Alex sent you a friend request."}));
    await tick();expect(screen.queryByText("Alex sent you a friend request.")).not.toBeInTheDocument();
});
test("invitation opens its section; polling while panel is open does not duplicate a notification",async()=>{
    render(app());await settle();invitations=[{id:9,gameId:42,inviter:"Alex"}];await tick();
    fireEvent.click(screen.getByRole("button",{name:"View"}));await settle();
    expect(screen.getByRole("heading",{name:"Game invitations"})).toBeInTheDocument();
    await tick();expect(screen.queryByText("Alex invited you to a game.")).not.toBeInTheDocument();
});
test("reconnection and failed reads establish silent baselines rather than replaying history",async()=>{
    const view=render(app());await settle();
    mockConnection={state:"reconnecting",generation:1};view.rerender(app());await settle();
    relationships=[{accountId:4,requestId:40,status:"INCOMING",username:"Offline"}];
    mockConnection={state:"connected",generation:2};view.rerender(app());await settle();await tick();
    expect(screen.queryByText("Offline sent you a friend request.")).not.toBeInTheDocument();
    global.fetch.mockRejectedValueOnce(new Error("Offline"));await tick();
    invitations=[{id:10,gameId:43,inviter:"While offline"}];await tick();
    expect(screen.queryByRole("region",{name:"Social notifications"})).not.toBeInTheDocument();
});
test("outgoing acceptance notifies once and account replacement clears notices and seen IDs",async()=>{
    relationships=[{accountId:2,requestId:20,status:"OUTGOING",username:"Alex"}];
    const view=render(app());await settle();
    relationships=[{accountId:2,requestId:20,status:"FRIEND",username:"Alex"}];await tick();
    expect(screen.getByText("Alex accepted your friend request.")).toBeInTheDocument();
    view.rerender(app("other-token"));await settle();
    expect(screen.queryByRole("region",{name:"Social notifications"})).not.toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/friends/relationships",expect.objectContaining({headers:expect.objectContaining({Authorization:"Bearer other-token"})}));
});
test("responses arriving after logout cannot publish a notice",async()=>{
    const view=render(app());await settle();let resolve;
    global.fetch.mockImplementationOnce(()=>new Promise(done=>{resolve=done;}));
    await tick();view.unmount();
    await act(async()=>resolve({ok:true,json:async()=>[]}));
    expect(screen.queryByRole("region",{name:"Social notifications"})).not.toBeInTheDocument();
});
test("failed invitation notifications do not disable the friends list",async()=>{
    global.fetch.mockImplementation(async url=>{
        if(url.endsWith("invitations")) throw new Error("Invitation service unavailable");
        return {ok:true,json:async()=>[]};
    });
    render(app());await settle();fireEvent.click(screen.getByRole("button",{name:"Friends",exact:true}));await settle();
    expect(screen.getByRole("heading",{name:"Your friends"})).toBeInTheDocument();
    expect(screen.queryByRole("region",{name:"Social notifications"})).not.toBeInTheDocument();
});

function ChatNoticeProbe({onView}) {
    const social=useSocial();
    return <><button onClick={()=>social.notifyChat(42,[{id:7,username:"Alex",text:"Hello"}],onView)}>Publish chat notice</button><button onClick={()=>social.clearChatNotifications(42)}>Leave chat</button></>;
}
test("chat notices share the existing notification area and View activates chat rather than Friends",async()=>{
    const onView=jest.fn();
    render(<MemoryRouter><SocialProvider jwt="token"><ChatNoticeProbe onView={onView}/></SocialProvider></MemoryRouter>);await settle();
    fireEvent.click(screen.getByRole("button",{name:"Publish chat notice"}));
    expect(screen.getByText("Alex: Hello")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button",{name:"View"}));expect(onView).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("dialog",{name:"Friends"})).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button",{name:"Publish chat notice"}));fireEvent.click(screen.getByRole("button",{name:"Leave chat"}));
    expect(screen.queryByRole("region",{name:"Social notifications"})).not.toBeInTheDocument();
});
test("a notice expires without blocking the board, but stays while its controls have keyboard focus",async()=>{
    render(app());await settle();invitations=[{id:19,gameId:42,inviter:"Alex"}];await tick();
    const view=screen.getByRole("button",{name:"View"});act(()=>view.focus());
    await act(async()=>{jest.advanceTimersByTime(15000);});expect(screen.getByText("Alex invited you to a game.")).toBeInTheDocument();
    act(()=>screen.getByRole("button",{name:"Friends",exact:true}).focus());
    await act(async()=>{jest.advanceTimersByTime(12000);});expect(screen.queryByText("Alex invited you to a game.")).not.toBeInTheDocument();
});

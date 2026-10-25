import React from "react";
import {act, fireEvent, render, screen, waitFor, within} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import {SocialProvider} from "./SocialContext";
import SocialToggle from "./SocialToggle";
import SocialView from "./SocialView";
import GameLayout from "../game/components/GameLayout";
jest.mock("../home", () => () => <h1>Home under panel</h1>);
const originalFetch = global.fetch;
let relationships, friends;
const reply = data => ({ok:true, headers:{get:()=>"application/json"}, json:async()=>data});
beforeEach(() => {
    localStorage.setItem("jwt",JSON.stringify("token"));
    localStorage.setItem("user",JSON.stringify({id:1,username:"self"}));
    friends=[];relationships=[];
    global.fetch=jest.fn().mockImplementation(async url => reply(url === "/api/v1/friends" ? friends : url === "/api/v1/friends/relationships" ? relationships : []));
});
afterEach(()=>{global.fetch=originalFetch;localStorage.clear();});
function show(children = <SocialToggle />) {
    return render(<MemoryRouter><SocialProvider jwt="token">{children}</SocialProvider></MemoryRouter>);
}
async function open() {
    show();const opener=screen.getByRole("button",{name:"Friends",exact:true});
    fireEvent.click(opener);
    const dialog=await screen.findByRole("dialog",{name:"Friends"});
    await screen.findByRole("heading",{name:"Your friends"});
    return {opener,dialog};
}
test("closed panel makes no private requests; opening loads authorized lists and Escape returns focus", async()=>{
    show();
    expect(global.fetch).not.toHaveBeenCalled();
    const opener=screen.getByRole("button",{name:"Friends",exact:true});
    opener.focus();fireEvent.click(opener);
    const dialog=await screen.findByRole("dialog",{name:"Friends"});
    expect(opener).toHaveAttribute("aria-expanded","true");
    expect(opener).toHaveAttribute("aria-describedby");
    await screen.findByRole("heading",{name:"Your friends"});
    fireEvent.keyUp(dialog,{key:"Escape",keyCode:27});
    await waitFor(()=>expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(opener).toHaveFocus();expect(opener).toHaveAttribute("aria-expanded","false");
});
test("search distinguishes self, sent, incoming and existing friends using account IDs", async()=>{
    relationships=[{accountId:2,status:"OUTGOING",requestId:20},{accountId:3,status:"INCOMING",requestId:30},{accountId:4,status:"FRIEND",requestId:40}];
    const {dialog}=await open();
    global.fetch.mockImplementation(async url=>reply(url.startsWith("/api/v1/users/players") ? {content:[{id:1,username:"self"},{id:2,username:"sent"},{id:3,username:"received"},{id:4,username:"friend"}],last:true} : url.endsWith("relationships") ? relationships : friends));
    fireEvent.click(within(dialog).getByRole("button",{name:"Search",exact:true}));
    fireEvent.change(screen.getByLabelText("Player name"),{target:{value:"player /?"}});
    fireEvent.click(screen.getByRole("button",{name:"Search players",exact:true}));
    expect(await screen.findByText("Request sent")).toBeInTheDocument();
    expect(screen.getByText("Already friends")).toBeInTheDocument();expect(screen.getByText("You")).toBeInTheDocument();
    expect(screen.queryByRole("button",{name:"Add friend"})).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button",{name:"Accept request"}));
    await waitFor(()=>expect(global.fetch).toHaveBeenCalledWith("/api/v1/friends/30/accept",expect.objectContaining({method:"PATCH"})));
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/users/players?page=0&size=5&username=player%20%2F%3F",expect.any(Object));
});
test("sending a request blocks duplicates and recovers Request sent from server data", async()=>{
    const {dialog}=await open();let finish;
    global.fetch.mockImplementation(async (url,options)=>{
        if(url.startsWith("/api/v1/users/players")) return reply({content:[{id:52,username:"candidate"}],last:true});
        if(options.method === "POST") return new Promise(resolve=>{finish=()=>{relationships=[{accountId:52,status:"OUTGOING",requestId:700}];resolve(reply({}));};});
        return reply(url.endsWith("relationships") ? relationships : friends);
    });
    fireEvent.click(within(dialog).getByRole("button",{name:"Search",exact:true}));
    fireEvent.change(screen.getByLabelText("Player name"),{target:{value:"candidate"}});
    fireEvent.click(screen.getByRole("button",{name:"Search players"}));
    const add=await screen.findByRole("button",{name:"Add friend"});fireEvent.click(add);fireEvent.click(add);
    expect(global.fetch.mock.calls.filter(([,options])=>options.method === "POST")).toHaveLength(1);
    await act(async()=>finish());
    expect(await screen.findByText("Request sent")).toBeInTheDocument();
    expect(screen.queryByRole("button",{name:"Add friend"})).not.toBeInTheDocument();
});
test("received requests retain accept and decline actions without reloading the page",async()=>{
    relationships=[{accountId:12,username:"requester",status:"INCOMING",requestId:900}];
    const {dialog}=await open();
    fireEvent.click(within(dialog).getByRole("button",{name:"Requests (1)"}));
    expect(screen.getByText("requester")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button",{name:"Decline"}));
    await waitFor(()=>expect(global.fetch).toHaveBeenCalledWith("/api/v1/friends/900/deny",expect.objectContaining({method:"DELETE"})));
});
test("inviting chooses an authorized existing lobby and sends the account ID",async()=>{
    friends=[{id:82,username:"invitee"}];const {dialog}=await open();
    global.fetch.mockImplementation(async(url)=>reply(url === "/api/v1/games/my" ? [{id:42,startDate:null,endDate:null,cancelled:false},{id:43,startDate:"started",endDate:null,cancelled:false}] : url.endsWith("relationships") ? [] : url === "/api/v1/friends" ? friends : {}));
    fireEvent.click(within(dialog).getByRole("button",{name:"Invite to lobby"}));
    const choose=await screen.findByLabelText("Choose a lobby");
    expect(screen.queryByRole("option",{name:"Game 43"})).not.toBeInTheDocument();
    fireEvent.change(choose,{target:{value:"42"}});fireEvent.click(screen.getByRole("button",{name:"Send invitation"}));
    await waitFor(()=>expect(global.fetch).toHaveBeenCalledWith("/api/v1/games/42/invite",expect.objectContaining({method:"POST",body:JSON.stringify({invited:82})})));
    expect(await screen.findByRole("status")).toHaveTextContent("Invitation sent.");
});
test("received game invitations and their acceptance remain available",async()=>{
    const {dialog}=await open();
    global.fetch.mockResolvedValue(reply([{id:8,gameId:42,inviter:"host"}]));
    fireEvent.click(within(dialog).getByRole("button",{name:"Invitations",exact:true}));
    expect(await screen.findByText("host: 42")).toBeInTheDocument();
    global.fetch.mockResolvedValue(reply({id:42}));
    fireEvent.click(screen.getByRole("button",{name:"Accept",exact:true}));
    await waitFor(()=>expect(global.fetch).toHaveBeenCalledWith("/api/v1/games/invitations/8/accept",expect.objectContaining({method:"PATCH"})));
    await waitFor(()=>expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
});
test("the old /friends URL opens the same panel and closes coherently to Home",async()=>{
    render(<MemoryRouter initialEntries={["/friends"]}><SocialProvider jwt="token"><button aria-controls="main-navigation">Menu</button><SocialToggle /><Routes><Route path="/friends" element={<SocialView />} /><Route path="/" element={<h1>Home route</h1>} /></Routes></SocialProvider></MemoryRouter>);
    const dialog=await screen.findByRole("dialog",{name:"Friends"});
    fireEvent.click(within(dialog).getByRole("button",{name:"Close",exact:true}));
    expect(await screen.findByRole("heading",{name:"Home route"})).toBeInTheDocument();
    await waitFor(()=>expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    await waitFor(()=>expect(screen.getByRole("button",{name:"Menu"})).toHaveFocus());
});
test("the game toolbar can open Friends without a general navbar",async()=>{
    show(<GameLayout gameId={42} game={{players:[],turns:[]}} role="Player"><button>Select card</button></GameLayout>);
    fireEvent.click(screen.getByRole("button",{name:"Friends",exact:true}));
    expect(await screen.findByRole("dialog",{name:"Friends"})).toBeInTheDocument();
    expect(screen.getByRole("button",{name:"Select card"})).toBeInTheDocument();
});

test("removing a friend requires confirmation and preserves the server action",async()=>{
    friends=[{id:52,username:"friend"}];await open();
    fireEvent.click(screen.getByRole("button",{name:"Remove",exact:true}));
    expect(global.fetch.mock.calls.every(([,options])=>options.method === "GET")).toBe(true);
    fireEvent.click(screen.getByRole("button",{name:"Cancel",exact:true}));
    expect(screen.queryByRole("button",{name:"Remove friend",exact:true})).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button",{name:"Remove",exact:true}));
    fireEvent.click(screen.getByRole("button",{name:"Remove friend",exact:true}));
    await waitFor(()=>expect(global.fetch).toHaveBeenCalledWith("/api/v1/friends/52/remove",expect.objectContaining({method:"DELETE"})));
});

test("load failure keeps the panel and offers a working refresh",async()=>{
    global.fetch.mockRejectedValue(new Error("Network unavailable"));show();
    fireEvent.click(screen.getByRole("button",{name:"Friends",exact:true}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Network unavailable");
    global.fetch.mockImplementation(async url=>reply(url === "/api/v1/friends" ? [{id:52,username:"recovered"}] : []));
    await waitFor(()=>expect(screen.getByRole("button",{name:"Refresh",exact:true})).toBeEnabled());
    fireEvent.click(screen.getByRole("button",{name:"Refresh",exact:true}));
    expect(await screen.findByText("recovered")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});


test("refresh also reloads game invitations while retaining the panel",async()=>{
    const {dialog}=await open();
    global.fetch.mockImplementation(async url=>reply(url === "/api/v1/games/invitations" ? [] : url.endsWith("relationships") ? relationships : friends));
    fireEvent.click(within(dialog).getByRole("button",{name:"Invitations",exact:true}));
    expect(await screen.findByText("No game invitations.")).toBeInTheDocument();
    global.fetch.mockImplementation(async url=>reply(url === "/api/v1/games/invitations" ? [{id:8,gameId:42,inviter:"host"}] : url.endsWith("relationships") ? relationships : friends));
    fireEvent.click(screen.getByRole("button",{name:"Refresh",exact:true}));
    expect(await screen.findByText("host: 42")).toBeInTheDocument();
});

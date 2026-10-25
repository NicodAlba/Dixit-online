import React from "react";
import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import {MemoryRouter, Routes, Route} from "react-router-dom";
import {SocialProvider} from "./SocialContext";
import SocialToggle from "./SocialToggle";
const originalFetch = global.fetch;
function show(friends, allowed = true) {
    global.fetch=jest.fn().mockImplementation(async (url, options) => ({ok: options.method !== "PATCH" || allowed, json: async () => options.method === "PATCH" ? {message:"Not eligible"} : url === "/api/v1/friends" ? friends : []}));
    render(<MemoryRouter><SocialProvider jwt="fixture-token"><SocialToggle /><Routes><Route path="/" element={<h1>Home</h1>} /><Route path="/games/42/spectate/17" element={<h1>Watching fixture</h1>} /></Routes></SocialProvider></MemoryRouter>);
    fireEvent.click(screen.getByRole("button",{name:"Friends",exact:true}));
}
afterEach(()=>{global.fetch=originalFetch; localStorage.clear();});
test("safe friend without private participation graph still renders",async()=>{
    show([{id:2,username:"Friend"}]);
    expect(await screen.findByText("Friend")).toBeInTheDocument();
    expect(screen.queryByRole("button",{name:"Spectate"})).not.toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/friends",expect.objectContaining({method:"GET"}));
});
test("authorized spectator link uses only safe game and player identifiers",async()=>{
    show([{id:2,username:"Friend",spectatableGame:{id:42,playerId:17}}]);
    fireEvent.click(await screen.findByRole("button",{name:"Spectate"}));
    expect(await screen.findByRole("heading",{name:"Watching fixture"})).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/games/42/spectate/17",expect.objectContaining({method:"PATCH"}));
});
test("server denial is integrated and does not enter spectator mode",async()=>{
    show([{id:2,username:"Friend",spectatableGame:{id:42,playerId:17}}],false);
    fireEvent.click(await screen.findByRole("button",{name:"Spectate"}));
    await waitFor(()=>expect(screen.getByRole("alert")).toHaveTextContent("Not eligible"));
    expect(screen.queryByRole("heading",{name:"Watching fixture"})).not.toBeInTheDocument();
});

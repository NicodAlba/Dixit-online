import React from "react";
import {act, fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import ActiveGames from "./ActiveGames";
const originalFetch = global.fetch;
const reply = data => ({ok: true, json: async () => data});
const lobby = {id: 42, startDate: null, endDate: null, cancelled: false, players: [{id: 900, user: {id: 71, username: "player"}}]};
afterEach(() => {global.fetch = originalFetch;});
function open() {
    render(<MemoryRouter><Routes><Route path="/" element={<ActiveGames jwt="token" />} />
        <Route path="/games/42" element={<h1>Existing lobby</h1>} />
        <Route path="/games/42/game" element={<h1>Existing game</h1>} /></Routes></MemoryRouter>);
}
test.each([false, true])("return revalidates authorized data and opens the current route (started=%s)", async started => {
    global.fetch = jest.fn().mockResolvedValueOnce(reply([lobby])).mockResolvedValueOnce(reply({...lobby, startDate: started ? "now" : null}));
    open();
    fireEvent.click(await screen.findByRole("button", {name: "Return to game"}));
    expect(await screen.findByRole("heading", {name: started ? "Existing game" : "Existing lobby"})).toBeInTheDocument();
    expect(global.fetch.mock.calls.map(([url]) => url)).toEqual(["/api/v1/games/my", "/api/v1/games/42"]);
    expect(global.fetch.mock.calls.every(([, options]) => !options.method || options.method === "GET")).toBe(true);
});
test("finished and cancelled games are excluded without imposing a one-game limit", async () => {
    global.fetch = jest.fn().mockResolvedValue(reply([lobby, {...lobby,id:43}, {...lobby,id:44,endDate:"closed"}, {...lobby,id:45,cancelled:true}]));
    open();
    fireEvent.click(await screen.findByRole("button", {name: "Return to game"}));
    const dialog = await screen.findByRole("dialog", {name: "Choose a game"});
    expect(dialog).toHaveTextContent("Game 42"); expect(dialog).toHaveTextContent("Game 43");
    expect(dialog).not.toHaveTextContent("Game 44"); expect(dialog).not.toHaveTextContent("Game 45");
    expect(global.fetch).toHaveBeenCalledTimes(1);
});
test.each(["cancelled", "finished", "deleted"])("return handles a game that was %s after discovery", async state => {
    global.fetch = jest.fn().mockResolvedValueOnce(reply([lobby])).mockResolvedValueOnce(state === "deleted" ? {ok:false,status:404} : reply({...lobby, cancelled:state === "cancelled",endDate:"now"}));
    open();
    fireEvent.click(await screen.findByRole("button", {name:"Return to game"}));
    expect(await screen.findByRole("alert")).toHaveTextContent(state === "cancelled" ? "This game was cancelled." : state === "finished" ? "This game has finished." : "This lobby is no longer available.");
    expect(screen.queryByRole("button", {name:"Return to game"})).not.toBeInTheDocument();
    expect(screen.queryByRole("heading")).not.toBeInTheDocument();
});
test("a late response for an old account cannot restore its games", async () => {
    let finish;
    global.fetch=jest.fn().mockReturnValueOnce(new Promise(resolve => {finish=resolve;})).mockResolvedValue(reply([]));
    const {rerender}=render(<MemoryRouter><ActiveGames jwt="old-token" /></MemoryRouter>);
    rerender(<MemoryRouter><ActiveGames jwt="new-token" /></MemoryRouter>);
    await waitFor(()=>expect(global.fetch).toHaveBeenCalledTimes(2));
    await act(async()=>finish(reply([lobby])));
    expect(screen.queryByRole("button",{name:"Return to game"})).not.toBeInTheDocument();
});


test("choosing among several games opens only the selected game", async () => {
    global.fetch=jest.fn().mockResolvedValueOnce(reply([lobby,{...lobby,id:43}])).mockResolvedValueOnce(reply(lobby));
    open();
    fireEvent.click(await screen.findByRole("button", {name:"Return to game"}));
    fireEvent.click(await screen.findByRole("button", {name:"Game 42 · Lobby"}));
    expect(await screen.findByRole("heading", {name:"Existing lobby"})).toBeInTheDocument();
    expect(global.fetch).toHaveBeenLastCalledWith("/api/v1/games/42", expect.objectContaining({headers:{Authorization:"Bearer token"}}));
});

test("a pending return cannot navigate after its account has unmounted", async () => {
    let finish;
    global.fetch=jest.fn().mockResolvedValueOnce(reply([lobby])).mockReturnValueOnce(new Promise(resolve=>{finish=resolve;}));
    const view=show=><MemoryRouter><Routes>
        <Route path="/" element={show ? <ActiveGames jwt="token" /> : <h1>Public home</h1>} />
        <Route path="/games/42" element={<h1>Wrong account game</h1>} />
    </Routes></MemoryRouter>;
    const {rerender}=render(view(true));
    fireEvent.click(await screen.findByRole("button", {name:"Return to game"}));
    rerender(view(false));
    await act(async()=>finish(reply(lobby)));
    expect(screen.getByRole("heading", {name:"Public home"})).toBeInTheDocument();
    expect(screen.queryByRole("heading", {name:"Wrong account game"})).not.toBeInTheDocument();
});

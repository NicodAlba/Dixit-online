import React from "react";
import {act, fireEvent, render, screen, waitFor} from "@testing-library/react";
import {Client} from "@stomp/stompjs";
import SockJS from "sockjs-client";
import Lobby from "./Lobby";
const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({useParams: () => ({gameId: "42"}), useNavigate: () => mockNavigate}));
jest.mock("../services/token.service", () => ({getLocalAccessToken: () => "token", getUser: () => ({username: "host"})}));
jest.mock("./InviteFriendList", () => ({InviteFriendList: () => null}));
jest.mock("@stomp/stompjs", () => ({Client: jest.fn()}));
jest.mock("sockjs-client", () => jest.fn());
const lobby = {id: 42, leader: {username: "host"}, startDate: null, players: [{user: {username: "host"}}]};
const joined = {...lobby, players: [...lobby.players, {user: {username: "guest"}}, {user: {username: "third"}}]};
let client;
const originalFetch = global.fetch;
beforeEach(() => {
    jest.clearAllMocks();
    client = {activate: jest.fn(), deactivate: jest.fn(), subscribe: jest.fn()};
    Client.mockImplementation(() => client);
    SockJS.mockImplementation(url => ({url}));
    global.fetch = jest.fn().mockResolvedValue({ok: true, json: async () => lobby});
});
afterEach(() => {global.fetch = originalFetch;});
async function openLobby() {
    render(<Lobby />);
    await screen.findByRole("button", {name: "Start"});
    return Client.mock.calls[0][0];
}
test("socket follows the current application origin", async () => {
    const options = await openLobby(); options.webSocketFactory();
    expect(SockJS).toHaveBeenLastCalledWith("/ws");
    expect(options.connectHeaders).toEqual({Authorization: "Bearer token"});
});
test("each connection attempt gets a new SockJS instance", async () => {
    const options = await openLobby();
    expect(options.webSocketFactory()).not.toBe(options.webSocketFactory());
});
test("subscription refreshes changes made during initial connection", async () => {
    const options = await openLobby();
    global.fetch.mockResolvedValue({ok: true, json: async () => joined});
    act(() => options.onConnect());
    await waitFor(() => expect(screen.getByRole("button", {name: "Start"})).toBeEnabled());
    expect(client.subscribe).toHaveBeenCalledWith("/topic/game/42", expect.any(Function));
    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(screen.getByText("third")).toBeInTheDocument();
});
test("membership and start invalidations reload the private lobby and navigate", async () => {
    const options = await openLobby(); await act(async () => options.onConnect());
    const receive = client.subscribe.mock.calls[0][1];
    global.fetch.mockResolvedValue({ok:true,json:async()=>joined});
    await act(async()=>receive({body:JSON.stringify({type:"GAME_UPDATED",gameId:42})}));
    await waitFor(()=>expect(screen.getByRole("button",{name:"Start"})).toBeEnabled());
    global.fetch.mockResolvedValue({ok:true,json:async()=>({...joined,startDate:"2026-10-23T12:00:00Z"})});
    await act(async()=>receive({body:JSON.stringify({type:"GAME_UPDATED",gameId:42})}));
    await waitFor(()=>expect(mockNavigate).toHaveBeenCalledWith("/games/42/game"));
});
test("REST start navigates without waiting for a socket message", async () => {
    global.fetch.mockResolvedValue({ok: true, json: async () => joined}); await openLobby();
    global.fetch.mockResolvedValue({ok: true, json: async () => ({...joined, startDate: "started"})});
    fireEvent.click(screen.getByRole("button", {name: "Start"}));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith("/games/42/game"));
    expect(global.fetch).toHaveBeenLastCalledWith("/api/v1/games/42/start", expect.objectContaining({method: "PATCH"}));
});

test("a delayed initial GET cannot overwrite a more recent membership message", async () => {
    let releaseInitial;
    global.fetch.mockImplementationOnce(() => new Promise(resolve => {releaseInitial = resolve;}))
        .mockResolvedValue({ok: true, json: async () => joined});
    render(<Lobby />);
    const options = Client.mock.calls[0][0];
    await act(async () => options.onConnect());
    const receive = client.subscribe.mock.calls[0][1];
    const newest = {...joined, players: [...joined.players, {user: {username: "fourth"}}]};
    global.fetch.mockResolvedValue({ok:true,json:async()=>newest});
    await act(async () => receive({body: JSON.stringify({type:"GAME_UPDATED",gameId:42})}));
    await screen.findByText("fourth");
    await act(async () => releaseInitial({ok: true, json: async () => lobby}));
    expect(screen.getByText("fourth")).toBeInTheDocument();
});

test("minimal deletion notification returns participants home with integrated feedback and no alert", async () => {
    const alert = jest.spyOn(window, "alert").mockImplementation(() => {});
    const options = await openLobby(); await act(async () => options.onConnect());
    const receive = client.subscribe.mock.calls[0][1];
    act(() => receive({body: JSON.stringify({type: "DELETED", gameId: 42})}));
    expect(alert).not.toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith("/", {replace: true, state: {notice: "The host left the lobby."}});
    alert.mockRestore();
});

test("a host's own leave ignores its deletion notification while the REST response is pending", async () => {
    const options = await openLobby(); await act(async () => options.onConnect());
    let finish;
    global.fetch.mockReturnValueOnce(new Promise(resolve => {finish = resolve;}));
    fireEvent.click(screen.getByRole("button", {name: "Leave", exact: true}));
    fireEvent.click(await screen.findByRole("button", {name: "Yes, leave"}));
    const receive = client.subscribe.mock.calls[0][1];
    act(() => receive({body: JSON.stringify({type: "DELETED", gameId: 42})}));
    await act(async () => finish({ok: true}));
    expect(mockNavigate.mock.calls).toEqual([["/"]]);
    expect(screen.queryByText("The host left the lobby.")).not.toBeInTheDocument();
});

test("a GET rejected after deletion cannot replace the closure with an HTTP error", async () => {
    const options = await openLobby(); await act(async () => options.onConnect());
    let finish;
    global.fetch.mockReturnValueOnce(new Promise(resolve => {finish = resolve;}));
    const receive = client.subscribe.mock.calls[0][1];
    act(() => receive({body: JSON.stringify({type: "GAME_UPDATED", gameId: 42})}));
    const signal = global.fetch.mock.calls.at(-1)[1].signal;
    act(() => receive({body: JSON.stringify({type: "DELETED", gameId: 42})}));
    expect(signal.aborted).toBe(true);
    await act(async () => finish({ok: false, status: 404}));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(mockNavigate).toHaveBeenCalledTimes(1);
});

test("an initially unknown lobby is not described as a host departure", async () => {
    global.fetch.mockResolvedValue({ok: false, status: 404});
    render(<Lobby />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Lobby not found. Check the game ID.");
    expect(mockNavigate).not.toHaveBeenCalled();
});

test("a missing previously loaded lobby has neutral feedback until a deletion event confirms the reason", async () => {
    const options = await openLobby(); await act(async () => options.onConnect());
    const receive = client.subscribe.mock.calls[0][1];
    global.fetch.mockResolvedValue({ok: false, status: 404});
    await act(async () => receive({body: JSON.stringify({type: "GAME_UPDATED", gameId: 42})}));
    expect(await screen.findByRole("alert")).toHaveTextContent("This lobby is no longer available.");
    expect(mockNavigate).not.toHaveBeenCalled();
    act(() => receive({body: JSON.stringify({type: "DELETED", gameId: 42})}));
    expect(mockNavigate).toHaveBeenCalledWith("/", {replace: true, state: {notice: "The host left the lobby."}});
});

test("initial connection feedback is delayed, but loss and recovery are shown immediately", async () => {
    jest.useFakeTimers();
    try {
        const options = await openLobby();
        expect(screen.queryByText("Connecting…")).not.toBeInTheDocument();
        act(() => jest.advanceTimersByTime(800));
        expect(screen.getByText("Connecting…")).toBeInTheDocument();
        await act(async () => options.onConnect());
        expect(screen.queryByText("Connecting…")).not.toBeInTheDocument();
        act(() => options.onWebSocketClose());
        expect(screen.getByText(/Reconnecting/)).toBeInTheDocument();
    } finally {jest.useRealTimers();}
});

test("a pending start is blocked and server failures remain visible in the lobby", async () => {
    global.fetch.mockResolvedValue({ok: true, json: async () => joined});
    await openLobby();
    let finish;
    global.fetch.mockReturnValueOnce(new Promise(resolve => {finish = resolve;}));
    const start = screen.getByRole("button", {name: "Start"});
    fireEvent.click(start); fireEvent.click(start);
    expect(start).toBeDisabled();
    expect(global.fetch.mock.calls.filter(([, options]) => options.method === "PATCH")).toHaveLength(1);
    await act(async () => finish({ok: false, json: async () => ({message: "Start rejected"})}));
    expect(screen.getByRole("alert")).toHaveTextContent("Start rejected");
    expect(start).toBeEnabled();
    expect(mockNavigate).not.toHaveBeenCalled();
});

test("the leave dialog can be dismissed without sending an action", async () => {
    await openLobby();
    fireEvent.click(screen.getByRole("button", {name: "Leave"}));
    expect(await screen.findByRole("dialog", {name: "Leave this lobby?"})).toBeInTheDocument();
    expect(screen.getByText(/Leaving deletes this lobby/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", {name: "Cancel"}));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(global.fetch.mock.calls.every(([, options]) => options.method === "GET")).toBe(true);
});

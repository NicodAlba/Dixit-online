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
test("membership and start messages update the lobby and navigate", async () => {
    const options = await openLobby(); await act(async () => options.onConnect());
    const receive = client.subscribe.mock.calls[0][1];
    act(() => receive({body: JSON.stringify(joined)}));
    expect(screen.getByRole("button", {name: "Start"})).toBeEnabled();
    act(() => receive({body: JSON.stringify({...joined, startDate: "2026-10-22T20:00:00Z"})}));
    expect(mockNavigate).toHaveBeenCalledWith("/games/42/game");
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
    act(() => receive({body: JSON.stringify(newest)}));
    await act(async () => releaseInitial({ok: true, json: async () => lobby}));
    expect(screen.getByText("fourth")).toBeInTheDocument();
});

test.each(['DELETED', '"DELETED"'])("deletion signal %s returns participants home", async (body) => {
    const oldAlert = window.alert;
    window.alert = jest.fn();
    try {
        const options = await openLobby(); await act(async () => options.onConnect());
        const receive = client.subscribe.mock.calls[0][1];
        act(() => receive({body}));
        expect(window.alert).toHaveBeenCalledWith("The leader deleted the game");
        expect(mockNavigate).toHaveBeenCalledWith("/");
    } finally {window.alert = oldAlert;}
});

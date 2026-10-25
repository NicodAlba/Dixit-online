jest.mock("./ActiveGames", () => () => null);
import React from "react";
import userEvent from "@testing-library/user-event";
import {act, fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import Home from "./index";
import {GameInvitationList} from "./GameInvitationList";

jest.mock("jwt-decode", () => () => ({authorities: ["PLAYER"]}));
const authenticationMessage = "Full authentication is required to access this resource.";
const invitation = {id: 7, gameId: 42, inviter: "host"};
const originalFetch = global.fetch;
function response(data, status = 200) {
    return {ok: status >= 200 && status < 300, status,
        headers: {get: () => "application/json"}, json: async () => data};
}
function session(token) {
    if (token) localStorage.setItem("jwt", JSON.stringify(token));
    else localStorage.removeItem("jwt");
}
function home() {
    return <MemoryRouter><Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<h1>Login page</h1>} />
        <Route path="/games/42" element={<h1>Game lobby</h1>} />
    </Routes></MemoryRouter>;
}
beforeEach(() => {localStorage.clear(); global.fetch = jest.fn();});
afterEach(() => {localStorage.clear(); global.fetch = originalFetch; jest.restoreAllMocks();});

test.each([false, true])("visitor with saved user=%s makes no private request and can reach login", async savedUser => {
    if (savedUser) localStorage.setItem("user", JSON.stringify({id: 1, username: "registered"}));
    global.fetch.mockResolvedValue(response({message: authenticationMessage}, 401));
    await act(async () => {render(home());});
    expect(screen.getByText("Do you want to play?")).toBeInTheDocument();
    expect(screen.queryByText(authenticationMessage)).not.toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", {name: "Play game"}));
    expect(await screen.findByRole("dialog", {name: "Join the story"})).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", {name: "Log in", exact: true}));
    expect(screen.getByRole("heading", {name: "Login page"})).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
});

test("authenticated visitor loads invitations and can accept one", async () => {
    session("token");
    global.fetch.mockResolvedValueOnce(response([invitation])).mockResolvedValueOnce(response({id: 42}));
    render(home());
    expect(await screen.findByText("host: 42")).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/games/invitations", {headers: {Authorization: "Bearer token"}});
    fireEvent.click(screen.getByRole("button", {name: "Accept"}));
    expect(await screen.findByRole("heading", {name: "Game lobby"})).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/games/invitations/7/accept", expect.objectContaining({method: "PATCH"}));
});

test.each([403, 500])("authenticated request retains server error feedback (%s)", async status => {
    session("token");
    global.fetch.mockResolvedValue(response({message: "Server rejected this request"}, status));
    render(home());
    expect(await screen.findByRole("alert")).toHaveTextContent("Server rejected this request");
});

test("authenticated network error remains visible", async () => {
    session("token"); global.fetch.mockRejectedValue(new Error("Network unavailable"));
    render(home());
    expect(await screen.findByRole("alert")).toHaveTextContent("Network unavailable");
});

test("mounted invitation component clears private data on session loss and loads on a new session", async () => {
    session("token-a"); global.fetch.mockResolvedValue(response([invitation]));
    const {rerender} = render(<MemoryRouter><GameInvitationList /></MemoryRouter>);
    expect(await screen.findByText("host: 42")).toBeInTheDocument();
    session(null);
    rerender(<MemoryRouter><GameInvitationList /></MemoryRouter>);
    expect(screen.queryByText("host: 42")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", {name: "Accept"})).not.toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(1);
    session("token-b"); global.fetch.mockResolvedValue(response([{id: 8, gameId: 43, inviter: "new-host"}]));
    rerender(<MemoryRouter><GameInvitationList /></MemoryRouter>);
    expect(await screen.findByText("new-host: 43")).toBeInTheDocument();
    expect(screen.queryByText("host: 42")).not.toBeInTheDocument();
    expect(global.fetch).toHaveBeenLastCalledWith("/api/v1/games/invitations", {headers: {Authorization: "Bearer token-b"}});
});

test("pending private response cannot reappear after session loss", async () => {
    session("token");
    let complete;
    global.fetch.mockReturnValue(new Promise(resolve => {complete = resolve;}));
    const {rerender} = render(<MemoryRouter><GameInvitationList /></MemoryRouter>);
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(1));
    session(null); rerender(<MemoryRouter><GameInvitationList /></MemoryRouter>);
    await act(async () => {complete(response([invitation]));});
    expect(screen.queryByText("host: 42")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(1);
});

test("switching authenticated sessions hides the previous account while new invitations load", async () => {
    session("token-a"); global.fetch.mockResolvedValueOnce(response([invitation]));
    const {rerender} = render(<MemoryRouter><GameInvitationList /></MemoryRouter>);
    expect(await screen.findByText("host: 42")).toBeInTheDocument();
    let complete;
    global.fetch.mockReturnValueOnce(new Promise(resolve => {complete = resolve;}));
    session("token-b"); rerender(<MemoryRouter><GameInvitationList /></MemoryRouter>);
    expect(screen.queryByText("host: 42")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", {name: "Accept"})).not.toBeInTheDocument();
    await act(async () => {complete(response([{id: 8, gameId: 43, inviter: "new-host"}]));});
    expect(await screen.findByText("new-host: 43")).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(2);
});


test("a lobby closure notice is integrated, dismissible and requires no visitor requests", async () => {
    render(<MemoryRouter initialEntries={[{pathname: "/", state: {notice: "The host left the lobby."}}]}><Home /></MemoryRouter>);
    expect(screen.getByRole("status")).toHaveTextContent("The host left the lobby.");
    fireEvent.click(screen.getByRole("button", {name: "Dismiss notification"}));
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
});


test("the public play dialog closes with Escape and restores focus to Play game", async () => {
    const user = userEvent.setup();
    render(home());
    const play = screen.getByRole("button", {name: "Play game"});
    await user.click(play);
    const dialog = await screen.findByRole("dialog", {name: "Join the story"});
    expect(dialog).toContainElement(document.activeElement);
    fireEvent.keyUp(dialog, {key: "Escape", keyCode: 27, which: 27});
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(play).toHaveFocus();
    expect(global.fetch).not.toHaveBeenCalled();
});

test("the public play dialog offers account creation without starting private requests", async () => {
    render(<MemoryRouter><Routes><Route path="/" element={<Home />} /><Route path="/register" element={<h1>Create your account</h1>} /></Routes></MemoryRouter>);
    fireEvent.click(screen.getByRole("button", {name: "Play game"}));
    await screen.findByRole("dialog", {name: "Join the story"});
    fireEvent.click(screen.getByRole("button", {name: "Create account", exact: true}));
    expect(await screen.findByRole("heading", {name: "Create your account"})).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
});

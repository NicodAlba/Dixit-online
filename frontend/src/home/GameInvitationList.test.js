import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import useFetchState from "../util/useFetchState";
import {GameInvitationList} from "./GameInvitationList";
const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({useNavigate: () => mockNavigate}));
jest.mock("../services/token.service", () => ({getLocalAccessToken: () => "token"}));
jest.mock("../util/useFetchState", () => jest.fn());
const originalFetch = global.fetch;
beforeEach(() => {
    jest.clearAllMocks();
    useFetchState.mockReturnValue([[{id: 7, gameId: 42, inviter: "host"}], jest.fn()]);
});
afterEach(() => {global.fetch = originalFetch;});
test("loads invitations from the current application origin", () => {
    render(<GameInvitationList />);
    expect(useFetchState).toHaveBeenCalledWith([], "/api/v1/games/invitations", "token", expect.any(Function), expect.any(Function), null, expect.any(Function));
});
test("failed acceptance displays the server reason and retains the invitation", async () => {
    global.fetch = jest.fn().mockResolvedValue({ok: false, status: 409, json: async () => ({message: "Already 4 players in the game"})});
    render(<GameInvitationList />); fireEvent.click(screen.getByRole("button", {name: "Accept"}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Already 4 players in the game");
    expect(screen.getByRole("button", {name: "Accept"})).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
});

test("network failures are visible and retain a valid invitation row", async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error("Network unavailable"));
    const {container} = render(<GameInvitationList />);
    expect(container.querySelector("tbody tbody")).toBeNull();
    expect(screen.getByRole("button", {name: "Accept"}).closest("tr")).toHaveTextContent("host: 42");
    fireEvent.click(screen.getByRole("button", {name: "Accept"}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Network unavailable");
    expect(screen.getByRole("button", {name: "Accept"})).toBeEnabled();
    expect(mockNavigate).not.toHaveBeenCalled();
});
test("successful acceptance navigates once while duplicate clicks are blocked", async () => {
    let finish;
    global.fetch = jest.fn().mockReturnValue(new Promise(resolve => {finish = resolve;}));
    render(<GameInvitationList />);
    const accept = screen.getByRole("button", {name: "Accept"});
    fireEvent.click(accept); fireEvent.click(accept);
    expect(accept).toBeDisabled();
    expect(global.fetch).toHaveBeenCalledTimes(1);
    await act(async () => finish({ok: true, json: async () => ({})}));
    expect(mockNavigate).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith("/games/42");
});

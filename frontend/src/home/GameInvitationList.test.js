import React from "react";
import {fireEvent, render, screen} from "@testing-library/react";
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

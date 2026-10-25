jest.mock("../social/SocialToggle", () => () => null);
import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import Game from "./Game";
let mockState;
jest.mock("react-router-dom", () => ({useNavigate: () => jest.fn()}));
jest.mock("../services/token.service", () => ({getLocalAccessToken: () => "token", getUser: () => ({id: 1, username: "me"})}));
jest.mock("../util/getIdFromUrl", () => () => "42");
jest.mock("../hooks/useGameTurnState", () => () => mockState);
jest.mock("../hooks/usePlayerNames", () => () => ({}));
jest.mock("./components/Chat", () => () => null);
const originalFetch = global.fetch;
beforeEach(() => {
    const me = {id: 20, user: {id: 1, username: "me"}, points: 0, cards: [{id: 7, image: {id: 2}}]};
    const narrator = {id: 21, user: {id: 2, username: "narrator"}, points: 0, handSize: 5};
    mockState = {game: {id: 42, players: [me, narrator], turns: [{id: 31}], winners: []},
        turn: {id: 31, state: "SHOWING_CARDS", playedCards: [{id: 8, image: {id: 3}, isOwn: true}, {id: 9, image: {id: 4}, isOwn: false}]},
        turnId: 31, players: [me, narrator], storyteller: narrator, refresh: jest.fn().mockResolvedValue(), connection: "connected", loading: false};
});
afterEach(() => {global.fetch = originalFetch;});
test("a pending vote cannot be duplicated and a rejection allows an explicit retry", async () => {
    let finish;
    global.fetch = jest.fn().mockReturnValueOnce(new Promise(resolve => {finish = resolve;}));
    render(<Game />);
    const vote = screen.getByRole("button", {name: "Vote for card 2"});
    expect(vote.tagName).toBe("BUTTON");
    fireEvent.click(vote); fireEvent.click(vote);
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("status")).toHaveTextContent("Sending vote");
    await act(async () => finish({ok: false, json: async () => ({message: "Please try again"})}));
    expect(screen.getByRole("alert")).toHaveTextContent("Please try again");
    global.fetch.mockResolvedValue({ok: true, json: async () => ({})});
    fireEvent.click(screen.getByRole("button", {name: "Vote for card 2"}));
    await act(async () => {});
    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(mockState.refresh).toHaveBeenCalledTimes(1);
});
test("reconnection keeps the hand and blocks actions until a fresh view is available", () => {
    mockState.connection = "reconnecting";
    global.fetch = jest.fn();
    const {rerender} = render(<Game />);
    expect(screen.getByRole("status")).toHaveTextContent("Your place is preserved");
    expect(screen.getByRole("button", {name: "Card 2"})).toBeDisabled();
    expect(screen.getByRole("button", {name: "Select card 1"})).toBeDisabled();
    expect(screen.getAllByAltText("Card").some(img => img.getAttribute("src") === "/Cards/2.png")).toBe(true);
    mockState = {...mockState, connection: "connected"}; rerender(<Game />);
    expect(screen.getByRole("button", {name: "Vote for card 2"})).toBeEnabled();
    expect(global.fetch).not.toHaveBeenCalled();
});

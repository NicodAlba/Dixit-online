jest.mock("../social/SocialToggle", () => () => null);
import React from "react";
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import Game from "./Game";
let mockState;
jest.mock("react-router-dom", () => ({useNavigate: () => jest.fn()}));
jest.mock("../services/token.service", () => ({getLocalAccessToken: () => "token", getUser: () => ({id: 1, username: "host"})}));
jest.mock("../util/getIdFromUrl", () => () => "42");
jest.mock("../hooks/useGameTurnState", () => () => mockState);
jest.mock("../hooks/usePlayerNames", () => () => ({}));
jest.mock("./components/LeftPlayerPanel", () => () => null);
jest.mock("./components/RightPlayerPanel", () => () => null);
jest.mock("./components/TopPlayerPanel", () => () => null);
jest.mock("./components/TurnStatusPanel", () => () => null);
jest.mock("./components/Chat", () => () => null);
const originalFetch = global.fetch;
afterEach(() => {global.fetch = originalFetch; jest.clearAllMocks();});
test("a reused image with a new card ID stays visible and selectable while the historical ID is hidden", async () => {
    const historical = {id: 10, image: {id: 7}}, replacement = {id: 100, image: {id: 7}};
    const player = {id: 20, user: {id: 1, username: "host"}, points: 0,
        cards: [historical, replacement, ...[2, 3, 4, 5, 6].map(i => ({id: 100 + i, image: {id: i}}))]};
    mockState = {refresh: jest.fn().mockResolvedValue(), game: {id: 42, turns: [{id: 30, playedCards: [historical]}]},
        turn: {id: 31, state: "WAITING_STORYTELLER_CARD", playedCards: []}, turnId: 31,
        setTurn: jest.fn(), players: [player], storyteller: player};
    global.fetch = jest.fn().mockResolvedValue({ok: true, json: async () => ({state: "WAITING_STORYTELLER_CLUE"})});
    render(<Game />);
    const images = screen.getAllByAltText("Card");
    expect(images).toHaveLength(6);
    expect(images.filter(img => img.getAttribute("src") === "/Cards/7.png")).toHaveLength(1);
    fireEvent.click(images.find(img => img.getAttribute("src") === "/Cards/7.png"));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith("/api/v1/turns/31/answer/100", expect.objectContaining({method: "PUT"})));
    expect(screen.getAllByAltText("Card")).toHaveLength(5);
});

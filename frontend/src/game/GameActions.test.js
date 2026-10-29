jest.mock("../social/SocialToggle", () => () => null);
import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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

function narratorWritingClue() {
    mockState = {...mockState, storyteller: mockState.players[0],
        turn: {id: 31, state: "WAITING_STORYTELLER_CLUE", clue: null, playedCards: []}};
    global.fetch = jest.fn();
}

test("the narrator sends the unchanged single-line clue contract with Enter", async () => {
    narratorWritingClue();
    global.fetch.mockResolvedValue({ok: true, json: async () => ({})});
    render(<Game />);
    const field = screen.getByRole("textbox", {name: "Your clue"});
    expect(field).toHaveAttribute("type", "text");
    expect(field).not.toHaveAttribute("maxlength");
    await userEvent.type(field, "An unexpected journey{enter}");
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/turns/31/clue", expect.objectContaining({method: "PUT", body: "An unexpected journey"}));
    expect(mockState.refresh).toHaveBeenCalledTimes(1);
});

test("a pending clue is disabled and duplicate form submission sends only once", async () => {
    narratorWritingClue();
    let complete;
    global.fetch.mockReturnValue(new Promise(resolve => {complete = resolve;}));
    render(<Game />);
    fireEvent.change(screen.getByRole("textbox", {name: "Your clue"}), {target: {value: "Beyond the clouds"}});
    const form = screen.getByRole("form", {name: "Storyteller clue"});
    fireEvent.submit(form); fireEvent.submit(form);
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(form).toHaveAttribute("aria-busy", "true");
    expect(screen.getByRole("textbox", {name: "Your clue"})).toBeDisabled();
    expect(screen.getByRole("button", {name: "Sending clue…"})).toBeDisabled();
    await act(async () => complete({ok: true, json: async () => ({})}));
    expect(form).toHaveAttribute("aria-busy", "false");
    expect(mockState.refresh).toHaveBeenCalledTimes(1);
});

test("a rejected clue keeps its draft and associates the error with the field for retry", async () => {
    narratorWritingClue();
    global.fetch.mockResolvedValueOnce({ok: false, json: async () => ({message: "Unable to save the clue. Please try again."})});
    render(<Game />);
    const field = screen.getByRole("textbox", {name: "Your clue"});
    fireEvent.change(field, {target: {value: "A secret door"}});
    fireEvent.click(screen.getByRole("button", {name: "Set clue"}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Unable to save the clue. Please try again.");
    expect(field).toHaveValue("A secret door");
    expect(field).toHaveAttribute("aria-invalid", "true");
    expect(field).toHaveAccessibleDescription(/Unable to save the clue/);
    expect(mockState.refresh).not.toHaveBeenCalled();
    global.fetch.mockResolvedValue({ok: true, json: async () => ({})});
    fireEvent.click(screen.getByRole("button", {name: "Set clue"}));
    await act(async () => {});
    expect(global.fetch).toHaveBeenCalledTimes(2);
    expect(global.fetch.mock.calls[1][1].body).toBe("A secret door");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

test("a network error keeps the clue and editing clears the obsolete error", async () => {
    narratorWritingClue();
    global.fetch.mockRejectedValue(new Error("Connection lost. Please try again."));
    render(<Game />);
    const field = screen.getByRole("textbox", {name: "Your clue"});
    fireEvent.change(field, {target: {value: "A distant light"}});
    fireEvent.click(screen.getByRole("button", {name: "Set clue"}));
    await screen.findByRole("alert");
    expect(field).toHaveValue("A distant light");
    fireEvent.change(field, {target: {value: "A distant lighthouse"}});
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(field).toHaveAttribute("aria-invalid", "false");
    expect(global.fetch).toHaveBeenCalledTimes(1);
});

test("fresh game snapshots and reconnection preserve the draft for the same round", () => {
    narratorWritingClue();
    const {rerender} = render(<Game />);
    fireEvent.change(screen.getByRole("textbox", {name: "Your clue"}), {target: {value: "Between sky and sea"}});
    mockState = {...mockState, game: {...mockState.game}, turn: {...mockState.turn, clue: null}, players: [...mockState.players]};
    rerender(<Game />);
    expect(screen.getByRole("textbox", {name: "Your clue"})).toHaveValue("Between sky and sea");
    expect(screen.queryByText("Clue: Between sky and sea")).not.toBeInTheDocument();
    mockState = {...mockState, connection: "reconnecting"}; rerender(<Game />);
    expect(screen.getByRole("textbox", {name: "Your clue"})).toBeDisabled();
    mockState = {...mockState, connection: "connected"}; rerender(<Game />);
    expect(screen.getByRole("textbox", {name: "Your clue"})).toHaveValue("Between sky and sea");
    expect(screen.getByRole("button", {name: "Set clue"})).toBeEnabled();
    expect(global.fetch).not.toHaveBeenCalled();
});

test("a new round clears the previous clue without sending it", () => {
    narratorWritingClue();
    const {rerender} = render(<Game />);
    fireEvent.change(screen.getByRole("textbox", {name: "Your clue"}), {target: {value: "An old story"}});
    mockState = {...mockState, turnId: 32, turn: {...mockState.turn, id: 32}};
    rerender(<Game />);
    expect(screen.getByRole("textbox", {name: "Your clue"})).toHaveValue("");
    expect(global.fetch).not.toHaveBeenCalled();
});

test("a blank or whitespace-only clue cannot be sent", () => {
    narratorWritingClue(); render(<Game />);
    expect(screen.getByRole("button", {name: "Set clue"})).toBeDisabled();
    fireEvent.change(screen.getByRole("textbox", {name: "Your clue"}), {target: {value: "   "}});
    fireEvent.submit(screen.getByRole("form", {name: "Storyteller clue"}));
    expect(global.fetch).not.toHaveBeenCalled();
});

test("only the narrator in the clue phase has a clue form", () => {
    narratorWritingClue();
    const {rerender} = render(<Game />);
    expect(screen.getByRole("textbox", {name: "Your clue"})).toBeInTheDocument();
    mockState = {...mockState, storyteller: mockState.players[1]}; rerender(<Game />);
    expect(screen.queryByRole("textbox", {name: "Your clue"})).not.toBeInTheDocument();
    mockState = {...mockState, storyteller: mockState.players[0], turn: {...mockState.turn, state: "WAITING_PLAYER_CARDS"}}; rerender(<Game />);
    expect(screen.queryByRole("form", {name: "Storyteller clue"})).not.toBeInTheDocument();
    mockState = {...mockState, turn: {...mockState.turn, state: "WAITING_STORYTELLER_CLUE"}, game: {...mockState.game, cancelled: true}}; rerender(<Game />);
    expect(screen.queryByRole("textbox", {name: "Your clue"})).not.toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
});

test("card presentation waits for acceptance and a rejected play retains the card",async()=>{
 mockState={...mockState,turn:{id:31,state:"WAITING_PLAYER_CARDS",playedCards:[]}};
 let finish;global.fetch=jest.fn(()=>new Promise(resolve=>{finish=resolve;}));
 const {container}=render(<Game />);
 fireEvent.click(screen.getByRole("button",{name:"Select card 1"}));
 expect(container.querySelector('.confirmed-card-flight')).toBeNull();
 expect(screen.getByRole("button",{name:"Select card 1"}).querySelector("img")).toHaveAttribute("src","/Cards/2.png");
 await act(async()=>finish({ok:false,json:async()=>({message:"Card rejected"})}));
 expect(screen.getByRole("alert")).toHaveTextContent("Card rejected");
 expect(screen.getByRole("button",{name:"Select card 1"})).toBeEnabled();
 expect(container.querySelector('.confirmed-card-flight')).toBeNull();
 global.fetch.mockResolvedValue({ok:true,json:async()=>({})});
 fireEvent.click(screen.getByRole("button",{name:"Select card 1"}));await act(async()=>{});
 expect(container.querySelector('.confirmed-card-flight img')).toHaveAttribute("src","/Cards/2.png");
 expect(mockState.refresh).toHaveBeenCalledTimes(1);
});

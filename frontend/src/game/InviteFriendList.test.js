import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import {InviteFriendList} from "./InviteFriendList";
import {useSocial} from "../social/SocialContext";
jest.mock("../social/SocialContext", () => ({useSocial: jest.fn()}));
const originalFetch = global.fetch;
let social;
beforeEach(() => {
    social = {friends: [{id: 3, username: "RiverBear"}, {id: 4, username: "MoonFox"}],
        loaded: true, loading: false, error: null, refresh: jest.fn(), openPanel: jest.fn(), request: jest.fn().mockResolvedValue(null),
        presence: {active: true, connections: {3: true, 4: false}, loading: false}};
    useSocial.mockImplementation(() => social);
    global.fetch = jest.fn();
});
afterEach(() => {global.fetch = originalFetch; jest.clearAllMocks();});

test("reuses social cards and presence without a table or another friends/presence request", () => {
    const {container} = render(<InviteFriendList game={{id: 42}} />);
    expect(screen.getByRole("heading", {name: "Friends"})).toBeInTheDocument();
    expect(container.querySelector("table")).toBeNull();
    expect(container.querySelectorAll(".social-person")).toHaveLength(2);
    expect(screen.getByLabelText("Connection status: Online")).toBeInTheDocument();
    expect(screen.getByLabelText("Connection status: Offline")).toBeInTheDocument();
    expect(screen.getByText("Invite to lobby #42")).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", {name: "Refresh friends"}));
    expect(social.refresh).toHaveBeenCalledTimes(1);
});

test("invites directly to the current lobby and does not duplicate a pending or accepted invitation", async () => {
    let finish;
    social.request.mockReturnValue(new Promise(resolve => {finish = resolve;}));
    render(<InviteFriendList game={{id: 42, password: "never-send-this"}} />);
    const invite = screen.getByRole("button", {name: "Invite: RiverBear"});
    fireEvent.click(invite); fireEvent.click(invite);
    expect(social.request).toHaveBeenCalledTimes(1);
    expect(social.request).toHaveBeenCalledWith("/api/v1/games/42/invite", "POST", {invited: 3});
    expect(screen.getByRole("button", {name: "Sending…: RiverBear"})).toBeDisabled();
    await act(async () => finish(null));
    expect(screen.getByRole("button", {name: "Invitation sent: RiverBear"})).toBeDisabled();
    expect(screen.getByRole("status")).toHaveTextContent("Invitation sent to RiverBear");
    expect(screen.queryByText(/Choose a lobby/)).not.toBeInTheDocument();
});

test("a rejected invitation keeps a visible error and allows an explicit retry", async () => {
    social.request.mockRejectedValueOnce(new Error("Game already started")).mockResolvedValueOnce(null);
    render(<InviteFriendList game={{id: 42}} />);
    fireEvent.click(screen.getByRole("button", {name: "Invite: RiverBear"}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Game already started");
    fireEvent.click(screen.getByRole("button", {name: "Retry invitation: RiverBear"}));
    expect(await screen.findByRole("status")).toHaveTextContent("Invitation sent to RiverBear");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

test("a friend already participating cannot be invited again", () => {
    render(<InviteFriendList game={{id: 42, players: [{user: {id: 3}}]}} />);
    expect(screen.getByRole("button", {name: "In this lobby: RiverBear"})).toBeDisabled();
    expect(screen.getByRole("button", {name: "Invite: MoonFox"})).toBeEnabled();
});

test.each([{id: 42, startDate: "today"}, {id: 42, players: [1,2,3,4].map(id => ({user: {id}}))}])("closed/full lobby disables invitations", game => {
    render(<InviteFriendList game={game} />);
    screen.getAllByRole("button").filter(button => button.textContent !== "").forEach(button => expect(button).toBeDisabled());
    expect(social.request).not.toHaveBeenCalled();
});

test("loading, load errors and empty states stay distinct and search opens the existing panel", () => {
    social.loaded = false; social.loading = true; social.friends = [];
    const {rerender} = render(<InviteFriendList game={{id: 42}} />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading friends");
    expect(screen.queryByText(/No friends yet/)).not.toBeInTheDocument();
    social.loading = false; social.error = "Friends unavailable";
    rerender(<InviteFriendList game={{id: 42}} />);
    expect(screen.getByRole("alert")).toHaveTextContent("Friends unavailable");
    social.error = null; social.loaded = true;
    rerender(<InviteFriendList game={{id: 42}} />);
    fireEvent.click(screen.getByRole("button", {name: "Find friends"}));
    expect(social.openPanel).toHaveBeenCalledWith("search");
});

test("a late response from the previous lobby never marks the new lobby as invited", async () => {
    let finish;
    social.request.mockReturnValue(new Promise(resolve => {finish = resolve;}));
    const {rerender} = render(<InviteFriendList game={{id: 42}} />);
    fireEvent.click(screen.getByRole("button", {name: "Invite: RiverBear"}));
    rerender(<InviteFriendList game={{id: 43}} />);
    await act(async () => finish(null));
    expect(screen.getByRole("button", {name: "Invite: RiverBear"})).toBeEnabled();
    expect(screen.queryByText("Invitation sent to RiverBear.")).not.toBeInTheDocument();
});

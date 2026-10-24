import React from "react";
import {act, fireEvent, render, screen, waitFor} from "@testing-library/react";
import useFetchState from "../util/useFetchState";
import {InviteFriendList} from "./InviteFriendList";
jest.mock("../services/token.service", () => ({getLocalAccessToken: () => "token"}));
jest.mock("../util/useFetchState", () => jest.fn());
const originalFetch = global.fetch;
beforeEach(() => {
    useFetchState.mockReturnValue([[{id: 3, username: "friend"}], jest.fn()]);
    global.fetch = jest.fn().mockResolvedValue({ok: true});
    jest.spyOn(window, "alert").mockImplementation(() => {});
});
afterEach(() => {global.fetch = originalFetch; jest.restoreAllMocks(); jest.clearAllMocks();});
test("friends and invitation requests follow the current application origin", async () => {
    render(<InviteFriendList game={{id: 42, password: null}} />);
    expect(useFetchState).toHaveBeenCalledWith([], "/api/v1/friends", "token", expect.any(Function), expect.any(Function));
    fireEvent.click(screen.getByRole("button", {name: "Invite"}));
    await waitFor(() => expect(global.fetch).toHaveBeenCalledWith("/api/v1/games/42/invite", expect.objectContaining({method: "POST"})));
});

test("friends use valid table rows and load errors are visible", () => {
    const {container} = render(<InviteFriendList game={{id: 42}} />);
    expect(container.querySelector("tbody tbody")).toBeNull();
    expect(screen.getByRole("button", {name: "Invite"}).closest("tr")).toHaveTextContent("friend");
    act(() => {
        const [, , , setMessage, setVisible] = useFetchState.mock.calls[0];
        setMessage("Friends unavailable"); setVisible(true);
    });
    expect(screen.getByRole("alert")).toHaveTextContent("Friends unavailable");
});
test("invitation failures remain visible and a pending request cannot be duplicated", async () => {
    let finish;
    global.fetch = jest.fn().mockReturnValue(new Promise(resolve => {finish = resolve;}));
    render(<InviteFriendList game={{id: 42}} />);
    const invite = screen.getByRole("button", {name: "Invite"});
    fireEvent.click(invite); fireEvent.click(invite);
    expect(invite).toBeDisabled();
    expect(global.fetch).toHaveBeenCalledTimes(1);
    await act(async () => finish({ok: false, status: 409, json: async () => ({message: "Game already started"})}));
    expect(screen.getByRole("alert")).toHaveTextContent("Game already started");
    expect(invite).toBeEnabled();
    global.fetch.mockResolvedValue({ok: true});
    fireEvent.click(invite);
    expect(await screen.findByRole("status")).toHaveTextContent("Invitation sent successfully");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

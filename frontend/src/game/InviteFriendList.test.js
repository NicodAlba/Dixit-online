import React from "react";
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
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

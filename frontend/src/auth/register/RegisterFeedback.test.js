import React from "react";
import {act, fireEvent, render, screen, waitFor} from "@testing-library/react";
import RegisterPlayer from "./index";
import tokenService from "../../services/token.service";

const originalFetch = global.fetch;
const originalLocation = window.location;
const response = (ok, data) => ({ok, json: async () => data});
beforeEach(() => {
    localStorage.clear();
    delete window.location;
    window.location = {assign: jest.fn()};
});
afterEach(() => {
    global.fetch = originalFetch;
    window.location = originalLocation;
    localStorage.clear();
});
function fillAndSubmit() {
    fireEvent.input(screen.getByLabelText("Username:"), {target: {value: "new-player"}});
    fireEvent.input(screen.getByLabelText("Password:"), {target: {value: "test-password"}});
    fireEvent.click(screen.getByRole("button", {name: "Create account", exact: true}));
}
test("registers once, logs in and establishes the session before navigating without an error", async () => {
    global.fetch = jest.fn().mockImplementation(async (url) => {
        expect(tokenService.getLocalAccessToken()).toBeNull();
        return url.endsWith("signup") ? response(true, {}) : response(true, {id: 71, token: "new-token", username: "new-player"});
    });
    window.location.assign.mockImplementation(() => {
        expect(tokenService.getLocalAccessToken()).toBe("new-token");
        expect(tokenService.getUser().id).toBe(71);
    });
    render(<RegisterPlayer />);
    fillAndSubmit();
    await waitFor(() => expect(window.location.assign).toHaveBeenCalledWith("/"));
    expect(global.fetch.mock.calls.map(([url]) => url)).toEqual(["/api/v1/auth/signup", "/api/v1/auth/signin"]);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
test("a rejected signup keeps the entered values and does not log in", async () => {
    global.fetch = jest.fn().mockResolvedValue(response(false, {message: "Username is already taken."}));
    render(<RegisterPlayer />);
    fillAndSubmit();
    expect(await screen.findByRole("alert")).toHaveTextContent("Username is already taken.");
    expect(screen.getByLabelText("Username:")).toHaveValue("new-player");
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(window.location.assign).not.toHaveBeenCalled();
});
test("automatic login failure keeps account context and retries login without registering again", async () => {
    global.fetch = jest.fn().mockResolvedValueOnce(response(true, {}))
        .mockResolvedValueOnce(response(false, {}))
        .mockResolvedValueOnce(response(true, {token: "retry-token"}));
    render(<RegisterPlayer />);
    fillAndSubmit();
    expect(await screen.findByRole("alert")).toHaveTextContent("Your account was created. Automatic login failed.");
    expect(screen.getByLabelText("Username:")).toHaveValue("new-player");
    expect(screen.getByLabelText("Username:")).toBeDisabled();
    expect(tokenService.getLocalAccessToken()).toBeNull();
    fireEvent.click(screen.getByRole("button", {name: "Try logging in"}));
    await waitFor(() => expect(window.location.assign).toHaveBeenCalledWith("/"));
    expect(global.fetch.mock.calls.map(([url]) => url)).toEqual(["/api/v1/auth/signup", "/api/v1/auth/signin", "/api/v1/auth/signin"]);
});
test("repeated submit while signup is pending cannot create duplicate requests", async () => {
    let resolveSignup;
    global.fetch = jest.fn().mockReturnValue(new Promise(resolve => {resolveSignup = resolve;}));
    render(<RegisterPlayer />);
    fillAndSubmit();
    fireEvent.submit(document.querySelector("form"));
    fireEvent.keyUp(document, {key: "Enter"});
    expect(global.fetch).toHaveBeenCalledTimes(1);
    await act(async () => resolveSignup(response(false, {message: "Please try again."})));
    expect(await screen.findByRole("alert")).toHaveTextContent("Please try again.");
});


test("a login transport failure leaves the created account available for retry", async () => {
    global.fetch = jest.fn().mockResolvedValueOnce(response(true, {})).mockRejectedValueOnce(new Error("Network unavailable"));
    render(<RegisterPlayer />);
    fillAndSubmit();
    expect(await screen.findByRole("alert")).toHaveTextContent("Your account was created. Network unavailable");
    expect(screen.getByRole("button", {name: "Try logging in"})).toBeEnabled();
    expect(tokenService.getLocalAccessToken()).toBeNull();
    expect(window.location.assign).not.toHaveBeenCalled();
});

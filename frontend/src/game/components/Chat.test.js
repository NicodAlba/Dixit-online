import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import Chat from "./Chat";

jest.mock("../../services/token.service", () => ({
    getLocalAccessToken: () => "token",
    getUser: () => null,
}));
const originalFetch = global.fetch;
const ok = data => ({ok: true, json: async () => data});
const deferred = () => {
    let resolve;
    const promise = new Promise(done => {resolve = done;});
    return {promise, resolve};
};
async function settle() {await act(async () => {});}
beforeEach(() => {
    jest.useFakeTimers();
    global.fetch = jest.fn().mockResolvedValue(ok([]));
});
afterEach(() => {
    global.fetch = originalFetch;
    jest.useRealTimers();
});

test("loads messages without requiring a cached user and stops polling on unmount", async () => {
    global.fetch.mockResolvedValue(ok([{username: "friend", text: "Hello"}]));
    const {unmount} = render(<Chat gameId={42} />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading chat");
    await settle();
    expect(screen.getByText("Hello")).toBeInTheDocument();
    const signal = global.fetch.mock.calls[0][1].signal;
    unmount();
    expect(signal.aborted).toBe(false); // Completed requests need no cancellation.
    act(() => jest.advanceTimersByTime(9000));
    expect(global.fetch).toHaveBeenCalledTimes(1);
});

test("reports a failed load and recovers on the next poll", async () => {
    global.fetch.mockResolvedValueOnce({ok: false, status: 503, json: async () => ({message: "Chat unavailable"})});
    render(<Chat gameId={42} />);
    await settle();
    expect(screen.getByRole("alert")).toHaveTextContent("Chat unavailable");
    await act(async () => jest.advanceTimersByTime(3000));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledTimes(2);
});

test("retains a failed draft, blocks duplicate sends and permits an explicit retry", async () => {
    const post = deferred();
    global.fetch.mockImplementation((url, options) => options.method === "POST" ? post.promise : Promise.resolve(ok([])));
    render(<Chat gameId={42} />);
    await settle();
    const input = screen.getByRole("textbox", {name: "Chat message"});
    fireEvent.change(input, {target: {value: "A clue for friends"}});
    const form = input.closest("form");
    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(screen.getByRole("button", {name: "Sending…"})).toBeDisabled();
    expect(global.fetch.mock.calls.filter(([, options]) => options.method === "POST")).toHaveLength(1);
    await act(async () => post.resolve({ok: false, status: 403, json: async () => ({message: "Message rejected"})}));
    expect(screen.getByRole("alert")).toHaveTextContent("Message rejected");
    expect(input).toHaveValue("A clue for friends");
    global.fetch.mockResolvedValue(ok([]));
    fireEvent.submit(form);
    await settle();
    expect(input).toHaveValue("");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(global.fetch.mock.calls.filter(([, options]) => options.method === "POST")).toHaveLength(2);
});

test("ignores and aborts an old game's response even when transport resolves after cancellation", async () => {
    const old = deferred();
    global.fetch.mockImplementation(url => url.endsWith("/42") ? old.promise : Promise.resolve(ok([{username: "new", text: "New game"}])));
    const {rerender} = render(<Chat gameId={42} />);
    const signal = global.fetch.mock.calls[0][1].signal;
    rerender(<Chat gameId={43} />);
    await settle();
    expect(signal.aborted).toBe(true);
    await act(async () => old.resolve(ok([{username: "old", text: "Old game"}])));
    expect(screen.getByText("New game")).toBeInTheDocument();
    expect(screen.queryByText("Old game")).not.toBeInTheDocument();
});

test("a completed send discards an older poll and refreshes without repeating POST", async () => {
    const oldPoll = deferred();
    global.fetch.mockResolvedValueOnce(ok([])).mockReturnValueOnce(oldPoll.promise)
        .mockResolvedValueOnce(ok({})).mockResolvedValueOnce(ok([{username: "me", text: "Sent"}]));
    render(<Chat gameId={42} />);
    await settle();
    act(() => jest.advanceTimersByTime(3000));
    fireEvent.change(screen.getByRole("textbox"), {target: {value: "Sent"}});
    fireEvent.click(screen.getByRole("button", {name: "Send"}));
    await settle();
    await act(async () => oldPoll.resolve(ok([])));
    expect(screen.getByText("Sent")).toBeInTheDocument();
    expect(global.fetch.mock.calls.filter(([, options]) => options.method === "POST")).toHaveLength(1);
});

test("a send finishing in an old game cannot erase the new game's draft", async () => {
    const post = deferred();
    global.fetch.mockImplementation((url, options) => options.method === "POST" ? post.promise : Promise.resolve(ok([])));
    const {rerender} = render(<Chat gameId={42} />);
    await settle();
    fireEvent.change(screen.getByRole("textbox"), {target: {value: "Old draft"}});
    fireEvent.click(screen.getByRole("button", {name: "Send"}));
    const signal = global.fetch.mock.calls.find(([, options]) => options.method === "POST")[1].signal;
    rerender(<Chat gameId={43} />);
    fireEvent.change(screen.getByRole("textbox"), {target: {value: "New draft"}});
    await act(async () => post.resolve(ok({})));
    expect(signal.aborted).toBe(true);
    expect(screen.getByRole("textbox")).toHaveValue("New draft");
});

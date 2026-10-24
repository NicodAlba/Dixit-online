import React from "react";
import {fireEvent, render, screen} from "@testing-library/react";
import Login from "./index";
jest.mock("../../components/formGenerator/formGenerator", () => {
    const React = require("react");
    return React.forwardRef((props, ref) => <button onClick={() => props.onSubmit({values: {username: "player", password: "test"}})}>Login</button>);
});
const originalFetch = global.fetch;
afterEach(() => {global.fetch = originalFetch;});
test("a network failure shows readable feedback instead of crashing the login screen", async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error("Network unavailable"));
    render(<Login />);
    fireEvent.click(screen.getByRole("button", {name: "Login"}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Network unavailable");
    expect(screen.getByRole("heading", {name: "Login"})).toBeInTheDocument();
});
test("a rejected login retains the form and its server-independent feedback", async () => {
    global.fetch = jest.fn().mockResolvedValue({status: 401});
    render(<Login />);
    fireEvent.click(screen.getByRole("button", {name: "Login"}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid login attempt");
    expect(screen.getByRole("button", {name: "Login"})).toBeInTheDocument();
});

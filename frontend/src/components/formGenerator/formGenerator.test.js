import React from "react";
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import FormGenerator from "./formGenerator";

test("submits the values currently visible after input or autofill without relying on a change event", async () => {
    const submit = jest.fn();
    render(<FormGenerator inputs={[
        {name: "username", tag: "Username", type: "text", validators: []},
        {name: "password", tag: "Password", type: "password", validators: []},
    ]} onSubmit={submit} buttonText="Login" />);
    fireEvent.input(screen.getByLabelText("Username:"), {target: {value: "player"}});
    fireEvent.input(screen.getByLabelText("Password:"), {target: {value: "visible-value"}});
    fireEvent.click(screen.getByRole("button", {name: "Login"}));
    await waitFor(() => expect(submit).toHaveBeenCalledWith({values: {username: "player", password: "visible-value"}}));
    expect(submit).toHaveBeenCalledTimes(1);
});

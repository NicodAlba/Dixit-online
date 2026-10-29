import React from "react";
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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

test("pressing Enter on the submit button sends the form only once", async () => {
    const user = userEvent.setup();
    const submit = jest.fn();
    render(<FormGenerator inputs={[
        {name: "username", tag: "Username", type: "text", validators: []},
    ]} onSubmit={submit} listenEnterKey buttonText="Create account" />);
    fireEvent.input(screen.getByLabelText("Username:"), {target: {value: "player"}});
    screen.getByRole("button", {name: "Create account"}).focus();
    await user.keyboard("{Enter}");
    await waitFor(() => expect(submit).toHaveBeenCalled());
    expect(submit).toHaveBeenCalledTimes(1);
});


test("Enter outside the form does not submit and unmount leaves no document listener", async () => {
    const submit = jest.fn();
    const {unmount} = render(<FormGenerator listenEnterKey onSubmit={submit} />);
    fireEvent.keyUp(document, {key: "Enter"});
    expect(submit).not.toHaveBeenCalled();
    unmount();
    fireEvent.keyUp(document, {key: "Enter"});
    expect(submit).not.toHaveBeenCalled();
});

test("a disabled form cannot submit with the keyboard", async () => {
    const submit = jest.fn();
    render(<FormGenerator disabled listenEnterKey onSubmit={submit} />);
    fireEvent.submit(document.querySelector("form"));
    expect(submit).not.toHaveBeenCalled();
});

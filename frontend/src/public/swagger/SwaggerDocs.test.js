import React from "react";
import {act, fireEvent, render, screen} from "@testing-library/react";
import SwaggerDocs from "./index";
import SwaggerUI from "swagger-ui-react";
jest.mock("swagger-ui-react", () => jest.fn(() => <div>Rendered API definition</div>));
const originalFetch = global.fetch;
const valid = {openapi: "3.1.0", info: {title: "Dixit", version: "1"}, paths: {}};
beforeEach(() => {jest.clearAllMocks(); SwaggerUI.mockImplementation(() => <div>Rendered API definition</div>);});
afterEach(() => {global.fetch = originalFetch;});

test("waits for a valid definition before mounting Swagger, without an external URL", async () => {
    let finish;
    global.fetch = jest.fn(() => new Promise(resolve => {finish = resolve;}));
    render(<SwaggerDocs />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading API documentation");
    expect(SwaggerUI).not.toHaveBeenCalled();
    await act(async () => finish({ok: true, json: async () => valid}));
    expect(screen.getByText("Rendered API definition")).toBeInTheDocument();
    expect(SwaggerUI.mock.calls[0][0]).toEqual({spec: valid});
    expect(global.fetch).toHaveBeenCalledWith("/v3/api-docs", expect.objectContaining({headers: {Accept: "application/json"}}));
});

test.each([
    ["404", {ok: false, status: 404, json: async () => ({message: "Resource not found."})}],
    ["invalid definition", {ok: true, json: async () => ({message: "Not a definition"})}],
    ["HTML", {ok: true, json: async () => {throw new SyntaxError("Invalid JSON");}}],
])("handles %s without sending the error payload to Swagger and allows retry", async (_, response) => {
    global.fetch = jest.fn().mockResolvedValueOnce(response).mockResolvedValueOnce({ok: true, json: async () => valid});
    render(<SwaggerDocs />);
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(SwaggerUI).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", {name: "Retry"}));
    expect(await screen.findByText("Rendered API definition")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

test("does not mount Swagger when an obsolete request completes after unmount", async () => {
    let finish;
    global.fetch = jest.fn(() => new Promise(resolve => {finish = resolve;}));
    const {unmount} = render(<SwaggerDocs />);
    const signal = global.fetch.mock.calls[0][1].signal;
    unmount();
    expect(signal.aborted).toBe(true);
    await act(async () => finish({ok: true, json: async () => valid}));
    expect(SwaggerUI).not.toHaveBeenCalled();
});

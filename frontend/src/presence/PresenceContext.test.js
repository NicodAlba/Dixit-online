import React from "react";
import {act, render, screen} from "@testing-library/react";
import {Client} from "@stomp/stompjs";
import SockJS from "sockjs-client";
import {PresenceProvider} from "./PresenceContext";
import {ParticipantPresence, ConnectionBadge} from "./ParticipantPresence";
jest.mock("@stomp/stompjs",()=>({Client:jest.fn()}));
jest.mock("sockjs-client",()=>jest.fn());
const originalFetch=global.fetch;
const reply=(online,status=200)=>({ok:status===200,status,json:async()=>status===200?[{accountId:71,online}]:{message:"Expired"}});
let clients;
beforeEach(()=>{
    jest.useFakeTimers();clients=[];jest.clearAllMocks();
    Client.mockImplementation(options=>{const client={activate:jest.fn(),deactivate:jest.fn(),publish:jest.fn(),options};clients.push(client);return client;});
    SockJS.mockImplementation(url=>({url}));global.fetch=jest.fn().mockResolvedValue(reply(true));
});
afterEach(()=>{jest.useRealTimers();global.fetch=originalFetch;});
function view(jwt="token") {return <PresenceProvider key={jwt} jwt={jwt}><ParticipantPresence gameId={42} players={[{id:901,user:{id:71,username:"Alpha"}}]}><ConnectionBadge accountId={71} /></ParticipantPresence></PresenceProvider>;}
async function connect(index=0) {await act(async()=>clients[index].options.onConnect());}
async function poll(){await act(async()=>jest.advanceTimersByTime(5000));}
test("a stored JWT is not shown as online and no presence request precedes CONNECT",async()=>{
    const {rerender}=render(view());expect(global.fetch).not.toHaveBeenCalled();
    expect(screen.queryByText("Online",{exact:true})).not.toBeInTheDocument();
    await act(async()=>clients[0].options.onStompError());
    expect(screen.getByLabelText("Connection status: Unknown")).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
    expect(clients[0].deactivate).toHaveBeenCalledTimes(1);
    await act(async()=>clients[0].options.onWebSocketClose());
    expect(screen.getByLabelText("Connection status: Unknown")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Connection authentication failed. Log in again.");
    rerender(view("fresh-token"));
    await connect(1);
    expect(screen.getByLabelText("Connection status: Online")).toBeInTheDocument();
    expect(global.fetch).toHaveBeenCalledWith("/api/v1/presence?gameId=42",expect.objectContaining({headers:{Authorization:"Bearer fresh-token"}}));
    expect(clients[0].publish).not.toHaveBeenCalled();
});
test("initial snapshots and unchanged presence are silent; one departure and return produce one notice each",async()=>{
    render(view());await connect();expect(screen.queryByRole("status")).not.toBeInTheDocument();
    await poll();expect(screen.queryByRole("status")).not.toBeInTheDocument();
    global.fetch.mockResolvedValue(reply(false));await poll();
    expect(screen.getByLabelText("Connection status: Offline")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Alpha disconnected. Their place is preserved.");
    await poll();expect(screen.getAllByText("Alpha disconnected. Their place is preserved.")).toHaveLength(1);
    global.fetch.mockResolvedValue(reply(true));await poll();
    expect(screen.getByRole("status")).toHaveTextContent("Alpha is back.");
    expect(screen.getAllByText("Alpha is back.")).toHaveLength(1);
});
test("an older offline response cannot overwrite a later online snapshot or announce a false departure",async()=>{
    let finish;
    global.fetch.mockReturnValueOnce(new Promise(resolve=>{finish=resolve;})).mockResolvedValue(reply(true));
    render(view());await connect();await poll();
    expect(screen.getByLabelText("Connection status: Online")).toBeInTheDocument();
    await act(async()=>finish(reply(false)));
    expect(screen.getByLabelText("Connection status: Online")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
});
test("reconnecting refreshes without announcing every participant as returned or replaying actions",async()=>{
    global.fetch.mockResolvedValue(reply(false));render(view());await connect();
    await act(async()=>clients[0].options.onWebSocketClose());
    expect(screen.queryByText("Offline",{exact:true})).not.toBeInTheDocument();
    global.fetch.mockResolvedValue(reply(true));await connect();
    expect(screen.getByLabelText("Connection status: Online")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(clients[0].publish).not.toHaveBeenCalled();
});
test("an authentication failure becomes unknown and recovery starts a silent fresh baseline",async()=>{
    global.fetch.mockResolvedValue(reply(false,401));render(view());await connect();
    expect(screen.getByLabelText("Connection status: Unknown")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Your session expired. Log in to reconnect.");
    global.fetch.mockResolvedValue(reply(true));await poll();
    expect(screen.getByLabelText("Connection status: Online")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
});
test("changing accounts closes the old connection and discards its pending snapshot",async()=>{
    let finish;global.fetch.mockReturnValueOnce(new Promise(resolve=>{finish=resolve;})).mockResolvedValue(reply(true));
    const {rerender}=render(view("old-token"));await connect();rerender(view("new-token"));
    expect(clients[0].deactivate).toHaveBeenCalledTimes(1);
    await connect(1);await act(async()=>finish(reply(false)));
    expect(screen.getByLabelText("Connection status: Online")).toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

import React from "react";
import {act, fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import {SocialProvider, useSocial} from "./SocialContext";
import {GameInvitationList} from "../home/GameInvitationList";
jest.mock("./SocialPanel", () => () => null);
jest.mock("./SocialNotifications", () => () => null);
const originalFetch = global.fetch, originalLocation = window.location;
const reply = (data, status=200) => ({ok:status<300,status,json:async()=>data});
const invitation = {id:7,gameId:42,inviter:"host"};
function Refresh() {const social=useSocial();return <button onClick={social.refresh}>Refresh social</button>;}
function show() {return render(<MemoryRouter><SocialProvider jwt="token"><GameInvitationList/><Refresh/></SocialProvider></MemoryRouter>);}
beforeAll(()=>{delete window.location;window.location={...originalLocation,replace:jest.fn()};});
afterAll(()=>{window.location=originalLocation;});
beforeEach(()=>{localStorage.setItem("jwt",JSON.stringify("token"));localStorage.setItem("user",JSON.stringify({id:1,username:"self"}));window.location.replace.mockClear();});
afterEach(()=>{global.fetch=originalFetch;localStorage.clear();});
test("Friends failure does not hide invitations that loaded successfully",async()=>{
    global.fetch=jest.fn(async url=>url.endsWith("/invitations") ? reply([invitation]) : reply({message:"Friends unavailable"},500));
    show();expect(await screen.findByText("Lobby 42")).toBeInTheDocument();
    expect(screen.getByRole("button",{name:"Accept"})).toBeEnabled();
});
test("simultaneous Friends and invitations 401 still clears the expired session",async()=>{
    global.fetch=jest.fn(async()=>reply({message:"Session expired"},401));
    show();await waitFor(()=>expect(localStorage.getItem("jwt")).toBeNull());
    expect(window.location.replace).toHaveBeenCalledWith("/");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
test("a transient invitation refresh failure retains the previous valid row",async()=>{
    global.fetch=jest.fn(async url=>reply(url.endsWith("/invitations") ? [invitation] : []));
    show();await screen.findByText("Lobby 42");
    global.fetch.mockImplementation(async url=>url.endsWith("/invitations") ? reply({message:"Please try again."},500) : reply([]));
    fireEvent.click(screen.getByRole("button",{name:"Refresh social"}));
    expect(await screen.findByRole("alert")).toHaveTextContent("Please try again.");
    expect(screen.getByText("Lobby 42")).toBeInTheDocument();
});
test("a delayed poll cannot resurrect an invitation retired during acceptance",async()=>{
    let finishPoll, finishAccept, invitationReads=0;
    global.fetch=jest.fn((url,options)=>{
        if(options.method === "PATCH") return new Promise(resolve=>{finishAccept=resolve;});
        if(url.endsWith("/invitations")) {
            if(++invitationReads===1) return Promise.resolve(reply([invitation]));
            return new Promise(resolve=>{finishPoll=resolve;});
        }
        return Promise.resolve(reply([]));
    });
    show();await screen.findByText("Lobby 42");
    fireEvent.click(screen.getByRole("button",{name:"Accept"}));
    fireEvent.click(screen.getByRole("button",{name:"Refresh social"}));
    await waitFor(()=>expect(invitationReads).toBe(2));
    await act(async()=>finishAccept(reply({message:"This invitation is no longer available."},410)));
    await screen.findByRole("alert");expect(screen.queryByText("Lobby 42")).not.toBeInTheDocument();
    await act(async()=>finishPoll(reply([invitation])));
    await waitFor(()=>expect(screen.queryByText("Lobby 42")).not.toBeInTheDocument());
});

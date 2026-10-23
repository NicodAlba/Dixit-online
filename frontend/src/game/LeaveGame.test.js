import React from "react";
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import Game from "./Game";
let mockState;const mockNavigate=jest.fn();
jest.mock("react-router-dom",()=>({useNavigate:()=>mockNavigate}));
jest.mock("../services/token.service",()=>({getLocalAccessToken:()=>"token",getUser:()=>({id:1,username:"host"})}));
jest.mock("../util/getIdFromUrl",()=>()=>"42");
jest.mock("../hooks/useGameTurnState",()=>()=>mockState);
jest.mock("../hooks/usePlayerNames",()=>()=>({}));
jest.mock("./components/Chat",()=>()=>null);
const originalFetch=global.fetch;
beforeEach(()=>{
 const host={id:20,user:{id:1,username:"host"},points:0,cards:[{id:7,image:{id:1}}]};
 mockState={game:{id:42,winners:[],turns:[]},turn:{id:31,state:"WAITING_STORYTELLER_CARD",playedCards:[]},turnId:31,players:[host],storyteller:host,refresh:jest.fn()};
 mockNavigate.mockClear();
});
afterEach(()=>{global.fetch=originalFetch;});
test("leave invokes the backend before navigating away",async()=>{
 global.fetch=jest.fn().mockResolvedValue({ok:true});render(<Game/>);fireEvent.click(screen.getByText("Leave game"));
 await waitFor(()=>expect(mockNavigate).toHaveBeenCalledWith("/"));
 expect(global.fetch).toHaveBeenCalledWith("/api/v1/games/42/leave",expect.objectContaining({method:"PATCH",headers:{Authorization:"Bearer token"}}));
});
test("a failed leave remains visible and does not navigate",async()=>{
 global.fetch=jest.fn().mockResolvedValue({ok:false,json:async()=>({message:"Server unavailable"})});render(<Game/>);fireEvent.click(screen.getByText("Leave game"));
 expect(await screen.findByRole("alert")).toHaveTextContent("Server unavailable");expect(mockNavigate).not.toHaveBeenCalled();
});
test("cancellation locks the hand without labelling it as a completed game or erasing cards",()=>{
 mockState={...mockState,game:{...mockState.game,endDate:"2026-10-23",cancelled:true}};
 global.fetch=jest.fn();render(<Game/>);expect(screen.getByRole("alert")).toHaveTextContent("Game cancelled");
 expect(screen.queryByText("GAME FINISHED")).not.toBeInTheDocument();fireEvent.click(screen.getByAltText("Card").parentElement);expect(global.fetch).not.toHaveBeenCalled();
});

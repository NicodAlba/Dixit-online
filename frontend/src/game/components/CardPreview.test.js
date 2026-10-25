import React from "react";
import {fireEvent,render,screen,waitFor} from "@testing-library/react";
import {OwnPlayerPanel} from "./OwnPlayerPanel";
import TurnStatusPanel from "./TurnStatusPanel";
import OpponentPanel from "./OpponentPanel";
const card={id:101,image:{id:7},isOwn:true};
const player={id:901,user:{id:71,username:"Fox"},cards:[card],points:0};
test("a locked hand can be inspected with focus and Escape without selecting or fetching another card",async()=>{
 const select=jest.fn();render(<OwnPlayerPanel currentPlayer={player} handCards={[card]} turn={{state:"SHOWING_CARDS",hasPlayed:true}} getPlayerName={p=>p.user.username} selectCard={select}/>);
 const inspect=screen.getByRole("button",{name:"Inspect hand card 1"});inspect.focus();fireEvent.click(inspect);
 const dialog=await screen.findByRole("dialog",{name:"hand card 1"});
 expect(screen.getByAltText("Enlarged illustration: hand card 1")).toHaveAttribute("src","/Cards/7.png");
 expect(select).not.toHaveBeenCalled();fireEvent.keyUp(dialog,{key:"Escape",keyCode:27});
 await waitFor(()=>expect(screen.queryByRole("dialog")).not.toBeInTheDocument());expect(inspect).toHaveFocus();
});
test("table inspection never votes and a phase change closes its obsolete preview",async()=>{
 const vote=jest.fn();const turn={id:31,state:"SHOWING_CARDS",playedCards:[card],hasVoted:true,ownVotedCardId:101};
 const {rerender}=render(<TurnStatusPanel game={{}} turn={turn} currentPlayer={player} voteCard={vote}/>);
 fireEvent.click(screen.getByRole("button",{name:"Inspect table card 1"}));expect(await screen.findByRole("dialog")).toBeInTheDocument();
 expect(vote).not.toHaveBeenCalled();
 rerender(<TurnStatusPanel game={{}} turn={{id:32,state:"WAITING_STORYTELLER_CARD",playedCards:[]}} currentPlayer={player} voteCard={vote}/>);
 expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});
test("public rival hand sizes never create inspection controls or actual illustrations",()=>{
 render(<OpponentPanel position="left" player={{...player,cards:[],handSize:6}} getPlayerName={p=>p.user.username}/>);
 expect(screen.queryByRole("button",{name:/Inspect/})).not.toBeInTheDocument();
 screen.getAllByAltText("Card").forEach(image=>expect(image).toHaveAttribute("src","/Cards/card_reversed.png"));
});

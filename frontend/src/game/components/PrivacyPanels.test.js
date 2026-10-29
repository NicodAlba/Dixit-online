import React from "react";
import {fireEvent, render, screen} from "@testing-library/react";
import TurnStatusPanel from "./TurnStatusPanel";
import SpectatorStatus from "../componentsSpectate/TurnStatusPanel";
import {OwnPlayerPanel} from "./OwnPlayerPanel";
import LeftPlayerPanel from "./LeftPlayerPanel";
const currentPlayer={id:20,user:{username:"me"},cards:[],points:0};
const cards=[{id:1,image:{id:7},isOwn:true},{id:2,image:{id:8},isOwn:false}];
const props={game:{winners:[]},currentPlayer,isStoryteller:false,storyteller:{id:30},getPlayerName:p=>p.user?.username,voteCard:jest.fn()};
beforeEach(()=>jest.clearAllMocks());
test("anonymous table uses isOwn and hasVoted without exposing a live vote count",()=>{
 const turn={state:"SHOWING_CARDS",playedCards:cards,hasVoted:false};
 const {rerender}=render(<TurnStatusPanel {...props} turn={turn}/>);
 const images=screen.getAllByAltText("Card");fireEvent.click(images[0].parentElement);expect(props.voteCard).not.toHaveBeenCalled();
 fireEvent.click(images[1].parentElement);expect(props.voteCard).toHaveBeenCalledWith(cards[1]);
 expect(screen.queryByText(/\d votes/)).not.toBeInTheDocument();
 props.voteCard.mockClear();rerender(<TurnStatusPanel {...props} turn={{...turn,hasVoted:true}}/>);
 fireEvent.click(screen.getAllByAltText("Card")[1].parentElement);expect(props.voteCard).not.toHaveBeenCalled();
});
test("closed round reveals vote totals to players and spectators and cannot vote",()=>{
 const turn={state:"FINISHED",playedCards:cards.map(c=>({...c,votes:[]}))};
 const {unmount}=render(<TurnStatusPanel {...props} turn={turn}/>);
 expect(screen.getAllByText("0 votes")).toHaveLength(2);fireEvent.click(screen.getAllByAltText("Card")[1].parentElement);expect(props.voteCard).not.toHaveBeenCalled();
 unmount();render(<SpectatorStatus {...props} turn={turn}/>);expect(screen.getAllByText("0 votes")).toHaveLength(2);
});
test("rival hand is drawn from public handSize without actual rival card identifiers",()=>{
 const players=[currentPlayer,{id:21,user:{username:"right"},cards:[],handSize:6},{id:22,user:{username:"left"},cards:[],handSize:5}];
 render(<LeftPlayerPanel currentPlayer={currentPlayer} players={players} getPlayerName={props.getPlayerName}/>);
 expect(screen.getAllByAltText("Card")).toHaveLength(5);screen.getAllByAltText("Card").forEach(img=>expect(img).toHaveAttribute("src","/Cards/card_reversed.png"));
});
test("already-played flag locks the hand even though anonymous cards lack owner fields",()=>{
 const selectCard=jest.fn();render(<OwnPlayerPanel currentPlayer={currentPlayer} handCards={[cards[1]]} turn={{state:"WAITING_PLAYER_CARDS",hasPlayed:true,playedCards:[]}} getPlayerName={props.getPlayerName} selectCard={selectCard}/>);
 fireEvent.click(screen.getByAltText("Card").parentElement);expect(selectCard).not.toHaveBeenCalled();
});

test("the persisted private choice marks Your vote after remount and does not allow changing it",()=>{
 const turn={state:"SHOWING_CARDS",playedCards:cards,hasVoted:true,ownVotedCardId:2};
 const first=render(<TurnStatusPanel {...props} turn={turn}/>);
 expect(screen.getByRole("button",{name:"Your vote: card 2"})).toBeDisabled();
 expect(screen.getByText("✓ Your vote")).toBeInTheDocument();
 fireEvent.click(screen.getByRole("button",{name:"Your vote: card 2"}));expect(props.voteCard).not.toHaveBeenCalled();
 first.unmount();render(<TurnStatusPanel {...props} turn={{...turn}}/>);
 expect(screen.getByText("✓ Your vote")).toBeInTheDocument();
 expect(screen.queryByText(/\d votes/)).not.toBeInTheDocument();
});
test("another perspective has no vote marker; an authorized spectator is labeled explicitly",()=>{
 const turn={state:"SHOWING_CARDS",playedCards:cards,hasVoted:false};
 const {rerender}=render(<TurnStatusPanel {...props} turn={turn}/>);
 expect(screen.queryByText(/Your vote/)).not.toBeInTheDocument();
 rerender(<SpectatorStatus {...props} turn={{...turn,hasVoted:true,ownVotedCardId:2}}/>);
 expect(screen.getByText("✓ Perspective vote")).toBeInTheDocument();
 expect(screen.getByRole("button",{name:"Perspective vote: card 2"})).toBeDisabled();
});

test("an authorized storyteller perspective does not give the spectator a clue form",()=>{
 render(<SpectatorStatus {...props} isStoryteller turn={{id:31,state:"WAITING_STORYTELLER_CLUE",clue:null,playedCards:[]}}/>);
 expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
 expect(screen.queryByRole("button",{name:"Set clue"})).not.toBeInTheDocument();
});

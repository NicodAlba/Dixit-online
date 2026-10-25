import React from "react";
import {render,screen} from "@testing-library/react";
import GameResult from "./GameResult";
const players=[{id:901,user:{id:71,username:"Fox"},points:16},{id:902,user:{id:72,username:"Owl"},points:10},{id:903,user:{id:73,username:"Bear"},points:9}];
const completed={id:42,endDate:"2026-10-25",cancelled:false,players,winners:[players[0]]};
beforeEach(()=>sessionStorage.clear());
test("a canonical winner celebrates once per game and keeps exact standings after reload",()=>{
 const first=render(<GameResult game={completed} currentPlayer={players[0]}/>);
 expect(screen.getByRole("heading",{name:"Your imagination shines!"})).toBeInTheDocument();
 expect(first.container.querySelector(".result-sparks")).not.toBeNull();
 expect(screen.getByRole("table",{name:"Final standings"})).toHaveTextContent("Fox · Winner16Owl10Bear9");
 first.unmount();const reloaded=render(<GameResult game={completed} currentPlayer={players[0]}/>);
 expect(reloaded.container.querySelector(".result-sparks")).toBeNull();
 expect(screen.getByText("16")).toBeInTheDocument();
});
test("all canonical tied winners are named and each receives a shared victory",()=>{
 const tied={...completed,winners:[players[0],players[1]],players:[players[0],{...players[1],points:16},players[2]]};
 render(<GameResult game={tied} currentPlayer={players[1]}/>);
 expect(screen.getByRole("heading",{name:"A shared victory!"})).toBeInTheDocument();
 expect(screen.getByText("Winners: Fox, Owl")).toBeInTheDocument();expect(screen.getAllByText("· Winner")).toHaveLength(2);
});
test("a nonwinner receives a warm result without celebration or changed points",()=>{
 const {container}=render(<GameResult game={completed} currentPlayer={players[2]}/>);
 expect(screen.getByRole("heading",{name:"Every story leaves a spark"})).toBeInTheDocument();
 expect(container.querySelector(".result-sparks")).toBeNull();expect(screen.getByText("9")).toBeInTheDocument();
});
test("a spectator stays neutral even when watching the winner's perspective",()=>{
 const {container}=render(<GameResult game={completed} currentPlayer={players[0]} readOnly/>);
 expect(screen.getByRole("heading",{name:"A story to remember"})).toBeInTheDocument();
 expect(container.querySelector(".result-sparks")).toBeNull();expect(screen.getByText("Winner: Fox")).toBeInTheDocument();
});
test("cancellation is not a defeat or a completed game and preserves scores",()=>{
 const {container}=render(<GameResult game={{...completed,cancelled:true}} currentPlayer={players[2]}/>);
 expect(screen.getByRole("region",{name:"Cancelled game scores"})).toBeInTheDocument();
 expect(screen.getByText("Game cancelled")).toBeInTheDocument();
 expect(screen.getByRole("table",{name:"Scores so far"})).toHaveTextContent("Fox16Owl10Bear9");
 expect(screen.queryByText("Winner: Fox")).not.toBeInTheDocument();expect(container.querySelector(".result-sparks")).toBeNull();
});

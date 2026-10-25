import React from "react";
import {act,fireEvent,render,screen,waitFor} from "@testing-library/react";
import StatsDashboard from "./StatsDashboard";
import GlobalStats from "./GlobalStats";
import Rankings from "./Rankings";
import UserStats from "./UserStats";
import AchievementList from "../achievements/achievementList";
let mockToken="token",mockAccount={id:71,authorities:["PLAYER"]};
jest.mock("../services/token.service",()=>({getLocalAccessToken:()=>mockToken}));
jest.mock("jwt-decode",()=>()=>mockAccount);
jest.mock("../util/getIdFromUrl",()=>()=>"71");
const originalFetch=global.fetch;
const ok=data=>({ok:true,status:200,json:async()=>data});
beforeEach(()=>{mockToken="token";mockAccount={id:71,authorities:["PLAYER"]};global.fetch=jest.fn().mockResolvedValue(ok({totalGamesPlayed:0}));});
afterEach(()=>{global.fetch=originalFetch;});
test("a visitor makes no private resource request and receives an English login state",async()=>{
 mockToken=null;render(<GlobalStats/>);expect(global.fetch).not.toHaveBeenCalled();expect(await screen.findByRole("alert")).toHaveTextContent("Log in to view this information.");
 expect(screen.getByRole("link",{name:"Log in"})).toHaveAttribute("href","/login");
});
test("HTTP failure stops loading and Retry recovers actual zero values",async()=>{
 global.fetch.mockResolvedValueOnce({ok:false,status:503,json:async()=>({message:"server detail"})}).mockResolvedValue(ok({totalGamesPlayed:0,averageGamesPerUser:0}));
 render(<GlobalStats/>);expect(screen.getByRole("status")).toHaveTextContent("Loading global statistics");
 expect(await screen.findByRole("alert")).toHaveTextContent("Unable to load this information.");
 fireEvent.click(screen.getByRole("button",{name:"Retry"}));
 expect(await screen.findByText("No completed games yet. Your first story will start the record.")).toBeInTheDocument();
 expect(screen.getByText("Average Games/User").closest(".stats-card")).toHaveTextContent("0.00");expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
test("ranking changes abort and discard old responses while keeping server order and rate formatting",async()=>{
 let finish;
 global.fetch.mockImplementation(url=>url.endsWith("/wins")?new Promise(resolve=>{finish=resolve;}):Promise.resolve(ok(url.endsWith("/points")?[{username:"Second",value:49},{username:"First",value:30}]:[{username:"Fox",value:.375}])));
 render(<Rankings/>);const signal=global.fetch.mock.calls[0][1].signal;
 fireEvent.change(screen.getByLabelText("Ranking type"),{target:{value:"points"}});
 expect(await screen.findByText("49")).toBeInTheDocument();expect(signal.aborted).toBe(true);
 await act(async()=>finish(ok([{username:"Obsolete",value:999}])));
 expect(screen.queryByText("Obsolete")).not.toBeInTheDocument();expect(screen.queryByText("999")).not.toBeInTheDocument();
 const rows=screen.getAllByRole("row");expect(rows[1]).toHaveTextContent("#1Second49");expect(rows[2]).toHaveTextContent("#2First30");
 fireEvent.change(screen.getByLabelText("Ranking type"),{target:{value:"win-rate"}});expect(await screen.findByText("37.5%")).toBeInTheDocument();
});
test("tabs load only the chosen view and support arrow, End and role-specific personal data",async()=>{
 global.fetch.mockImplementation(url=>Promise.resolve(ok(url.includes("rankings")?[]:{})));
 render(<StatsDashboard/>);await screen.findByText("Total Games Played");expect(global.fetch).toHaveBeenCalledTimes(1);
 const globalTab=screen.getByRole("tab",{name:"Global statistics"});globalTab.focus();fireEvent.keyDown(globalTab,{key:"ArrowRight"});
 expect(await screen.findByText("No ranking data available yet.")).toBeInTheDocument();expect(screen.getByRole("tab",{name:"Rankings"})).toHaveFocus();
 fireEvent.keyDown(screen.getByRole("tab",{name:"Rankings"}),{key:"End"});
 expect(await screen.findByText("General performance")).toBeInTheDocument();expect(global.fetch).toHaveBeenCalledWith("/api/v1/stats/user/71",expect.anything());
});
test("ADMIN has no personal tab and no personal request",async()=>{
 mockAccount={id:90,authorities:["ADMIN"]};render(<StatsDashboard/>);await screen.findByText("Total Games Played");expect(screen.queryByRole("tab",{name:"My statistics"})).not.toBeInTheDocument();
 expect(global.fetch.mock.calls.every(([url])=>url==="/api/v1/stats/global")).toBe(true);
});
test("personal statistics preserve exact values and replace broken images using React",async()=>{
 global.fetch.mockResolvedValue(ok({gamesPlayed:8,gamesWon:3,totalPoints:37,winRate:.375,averagePointsPerGame:4.625,deceptionRate:.125,favouriteCard:{url:"/Cards/7.png"}}));
 render(<UserStats userId={71}/>);await screen.findByText("General performance");
 expect(screen.getByText("Win Rate").closest(".stats-card")).toHaveTextContent("37.50%");
 expect(screen.getByText("Average Points Per Game").closest(".stats-card")).toHaveTextContent("4.63");
 expect(screen.getByText("Deception Rate").closest(".stats-card")).toHaveTextContent("12.50%");
 fireEvent.error(screen.getByAltText("Your most played illustration"));expect(screen.getByText("Image unavailable")).toBeInTheDocument();expect(screen.getByText("Total Points").closest(".stats-card")).toHaveTextContent("37");
});
test("a changed session discards an old resource even if it resolves after cancellation",async()=>{
 let finish;global.fetch.mockReturnValueOnce(new Promise(resolve=>{finish=resolve;})).mockResolvedValue(ok({totalPoints:55}));
 const {rerender}=render(<UserStats userId={71}/>);const signal=global.fetch.mock.calls[0][1].signal;
 mockToken="fresh-token";rerender(<UserStats userId={71}/>);await screen.findByText("Total Points");expect(signal.aborted).toBe(true);
 await act(async()=>finish(ok({totalPoints:999})));expect(screen.queryByText("999")).not.toBeInTheDocument();expect(screen.getByText("55")).toBeInTheDocument();
});
test("achievement loading differs from empty, uses the current JWT and exposes unchanged goals and English dates",async()=>{
 let finish;mockToken="fresh-token";global.fetch.mockReturnValue(new Promise(resolve=>{finish=resolve;}));render(<AchievementList/>);
 expect(screen.getByRole("status")).toHaveTextContent("Loading achievements");expect(screen.queryByText("No achievements available yet.")).not.toBeInTheDocument();
 await act(async()=>finish(ok([{id:1,name:"First story",description:"Play 1 game",threshold:1,dateObtained:"2026-10-25T12:00:00Z",badgeImage:{url:"/Achievements/PlayAchievements/Play1.png"}},{id:2,name:"Five stories",description:"Play 5 games",threshold:5,dateObtained:null}])));
 expect(global.fetch).toHaveBeenCalledWith("/api/v1/users/71/achievements",expect.objectContaining({headers:{Authorization:"Bearer fresh-token"}}));
 expect(screen.getByAltText("Earned achievement: First story")).toHaveAttribute("src","/Achievements/PlayAchievements/Play1.png");
 const details=screen.getByLabelText("Details for Five stories");fireEvent.click(details);expect(details.closest("details")).toHaveAttribute("open");expect(details.closest("details")).toHaveTextContent("Threshold5");
 expect(screen.getByText("25 Oct 2026")).toBeInTheDocument();
});

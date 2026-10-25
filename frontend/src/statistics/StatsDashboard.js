import React, {useRef,useState} from "react";
import jwt_decode from "jwt-decode";
import {FiBarChart2,FiAward,FiUser} from "react-icons/fi";
import tokenService from "../services/token.service";
import Tooltip from "../components/ui/Tooltip";
import GlobalStats from "./GlobalStats";
import UserStats from "./UserStats";
import Rankings from "./Rankings";
import "../static/css/records.css";
import "./Stats.css";

export default function StatsDashboard() {
    const [active,setActive]=useState("global");const buttons=useRef([]);
    let account={};try{const jwt=tokenService.getLocalAccessToken();if(jwt)account=jwt_decode(jwt);}catch{/* The server remains the authority. */}
    const tabs=[{id:"global",label:"Global statistics",Icon:FiBarChart2},{id:"rankings",label:"Rankings",Icon:FiAward},...(account.authorities?.includes("PLAYER")?[{id:"personal",label:"My statistics",Icon:FiUser}]:[])];
    const current=tabs.some(tab=>tab.id===active)?active:"global";
    function keyboard(event,index){
        let next;
        if(event.key==="ArrowRight")next=(index+1)%tabs.length;
        if(event.key==="ArrowLeft")next=(index+tabs.length-1)%tabs.length;
        if(event.key==="Home")next=0;if(event.key==="End")next=tabs.length-1;
        if(next==null)return;event.preventDefault();setActive(tabs[next].id);buttons.current[next]?.focus();
    }
    return <main className="records-page statistics-page"><div className="records-shell">
        <header className="records-header"><p>Your story in numbers</p><h1 className="records-title">Statistics</h1><p>Every story leaves a mark.</p></header>
        <div className="stats-tabs" role="tablist" aria-label="Statistics views">{tabs.map(({id,label,Icon},index)=><Tooltip key={id} text={label}><button ref={button=>{buttons.current[index]=button;}} type="button" role="tab" id={`stats-tab-${id}`} aria-controls={`stats-panel-${id}`} aria-selected={current===id} tabIndex={current===id?0:-1} onClick={()=>setActive(id)} onKeyDown={event=>keyboard(event,index)}><Icon aria-hidden="true"/>{label}</button></Tooltip>)}</div>
        <section className="records-panel stats-content" role="tabpanel" tabIndex={0} id={`stats-panel-${current}`} aria-labelledby={`stats-tab-${current}`}>
            {current==="global"?<GlobalStats/>:current==="rankings"?<Rankings/>:<UserStats userId={account.id}/>}
        </section>
    </div></main>;
}

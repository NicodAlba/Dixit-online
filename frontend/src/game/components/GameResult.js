import React, {useEffect, useState} from "react";
import {FiAward, FiCompass, FiHeart, FiStar} from "react-icons/fi";

function ResultEmblem({gameId, accountId, celebrate, Icon}) {
    const key=`dixit-result:${gameId}:${accountId}`;
    const eligible=celebrate && gameId!=null && accountId!=null;
    const [active,setActive]=useState(()=>{try{return eligible && sessionStorage.getItem(key)!=="seen";}catch{return false;}});
    useEffect(()=>{if(eligible)try{sessionStorage.setItem(key,"seen");}catch{setActive(false);/* Static results remain accessible without storage. */}},[key,eligible]);
    return <>
        {active && <div className="result-sparks" aria-hidden="true">{Array.from({length:12},(_,i)=><span key={i} style={{"--spark":i}}>✦</span>)}</div>}
        <div className={`result-emblem ${active?"is-celebrating":""}`} aria-hidden="true"><Icon /></div>
    </>;
}
export default function GameResult({game,currentPlayer,readOnly=false}) {
    if(!game.endDate && !game.cancelled)return null;
    const winners=game.cancelled?[]:game.winners||[];
    const won=!readOnly && winners.some(player=>player.user?.id!=null && player.user.id===currentPlayer?.user?.id);
    const title=game.cancelled?"Until the next story":readOnly?"A story to remember":won?(winners.length>1?"A shared victory!":"Your imagination shines!"):"Every story leaves a spark";
    const Icon=game.cancelled?FiCompass:won?FiAward:readOnly?FiStar:FiHeart;
    const standings=[...(game.players||[])].sort((a,b)=>(b.points||0)-(a.points||0));
    return <section className={`game-result result-${game.cancelled?"cancelled":readOnly?"spectator":won?"winner":"player"}`} aria-label={game.cancelled?"Cancelled game scores":"Final results"}>
        <ResultEmblem key={`${game.id}:${currentPlayer?.user?.id}:${won}`} gameId={game.id} accountId={currentPlayer?.user?.id} celebrate={won} Icon={Icon} />
        <p className="result-overline" role="status">{game.cancelled?"Game cancelled":"Game finished"}</p>
        <h2>{title}</h2>
        <p>{game.cancelled?"Your scores and history are preserved. No winners or new rewards are awarded.":readOnly?"Thank you for watching this world of imagination.":won?"A wonderful ending to a game of unexpected connections.":"Thank you for the imagination you brought to the table."}</p>
        {!game.cancelled && <p className="result-winners">{winners.length>1?"Winners":"Winner"}: {winners.map(player=>player.user?.username||"Unknown player").join(", ")||"No winner recorded"}</p>}
        <table className="result-standings"><caption>{game.cancelled?"Scores so far":"Final standings"}</caption><thead><tr><th scope="col">Player</th><th scope="col">Points</th></tr></thead><tbody>
            {standings.map(player=><tr key={player.id}><th scope="row">{player.user?.username||"Unknown player"}{!game.cancelled&&winners.some(winner=>winner.id===player.id)&&<span className="result-winner-label"> · Winner</span>}</th><td>{player.points??0}</td></tr>)}
        </tbody></table>
    </section>;
}

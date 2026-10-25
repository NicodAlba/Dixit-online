import {useState} from "react";
import useAuthorizedResource from "../hooks/useAuthorizedResource";
import ResourceFeedback from "../components/ui/ResourceFeedback";
const options=[{value:"wins",label:"Wins"},{value:"points",label:"Total Points"},{value:"avg-points",label:"Average Points"},{value:"trap-votes",label:"Trap Votes"},{value:"win-rate",label:"Win Rate"},{value:"deception-rate",label:"Deception Rate"}];
export default function Rankings(){
    const [type,setType]=useState("wins");const resource=useAuthorizedResource(`/api/v1/stats/rankings/${type}`);
    const data=Array.isArray(resource.data)?resource.data:[];
    return <><div className="ranking-controls"><h2 className="section-title">Ranking: {options.find(option=>option.value===type).label}</h2><label htmlFor="rankingSelector">Ranking type<select id="rankingSelector" value={type} onChange={event=>setType(event.target.value)}>{options.map(option=><option key={option.value} value={option.value}>{option.label}</option>)}</select></label></div>
        {resource.loading||resource.error?<ResourceFeedback resource={resource} label="rankings"/>:data.length===0?<p className="records-feedback">No ranking data available yet.</p>:<div className="ranking-scroll"><table className="ranking-table"><caption>{options.find(option=>option.value===type).label} ranking</caption><thead><tr><th scope="col">Rank</th><th scope="col">User</th><th scope="col">Value</th></tr></thead><tbody>{data.map((row,index)=><tr key={`${row.username}:${index}`}><td>#{index+1}</td><th scope="row">{row.username}</th><td>{type.includes("rate")?`${(row.value*100).toFixed(1)}%`:row.value}</td></tr>)}</tbody></table></div>}
    </>;
}

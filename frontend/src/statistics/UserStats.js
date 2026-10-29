import {useState} from "react";
import {Row,Col} from "reactstrap";
import useAuthorizedResource from "../hooks/useAuthorizedResource";
import ResourceFeedback from "../components/ui/ResourceFeedback";
import StatCard from "./StatCard";
function FavouriteCard({card}) {
    const [failed,setFailed]=useState(false);const source=card?.path||card?.imagePath||card?.url;
    return <div className="stats-card stats-favourite"><h3 className="stats-card-title">Favourite Card</h3>{source&&!failed?<img src={source} alt="Your most played illustration" className="favourite-card-img" onError={()=>setFailed(true)}/>:<p>{source?"Image unavailable":"No card data yet"}</p>}</div>;
}
export default function UserStats({userId}){
    const resource=useAuthorizedResource(userId?`/api/v1/stats/user/${userId}`:null),stats=resource.data;
    if(resource.loading||resource.error)return <ResourceFeedback resource={resource} label="your statistics"/>;
    if(!stats)return <p className="records-feedback">No statistics available.</p>;
    const groups=[{title:"General performance",items:[["Total Games Played",stats.gamesPlayed??0],["Games Won",stats.gamesWon??0],["Win Rate",((stats.winRate??0)*100).toFixed(2),"%"]]},
        {title:"Scoring",items:[["Total Points",stats.totalPoints??0],["Average Points Per Game",(stats.averagePointsPerGame??0).toFixed(2)]]},
        {title:"Strategy & favourites",favourite:true,items:[["Deceptions Made",stats.trapVotesReceived??0],["Deception Rate",((stats.deceptionRate??0)*100).toFixed(2),"%"]]},
        {title:"Time analysis",size:"3",items:[["Total Time Played",stats.totalDuration??0,"min"],["Avg Game Duration",(stats.averageDuration??0).toFixed(2),"min"],["Longest Game",stats.maxDuration??0,"min"],["Shortest Game",stats.minDuration??0,"min"]]}];
    return <>{stats.gamesPlayed===0&&<p className="records-feedback">No completed games yet. Your statistics will grow with your stories.</p>}{groups.map(group=><section key={group.title}><h2 className="section-title">{group.title}</h2><Row className="justify-content-center">{group.items.map(([title,value,unit])=><StatCard key={title} title={title} value={value} unit={unit} colSize={group.size}/>)}{group.favourite&&<Col md="4" sm="6" className="mb-3"><FavouriteCard key={stats.favouriteCard?.url||"empty"} card={stats.favouriteCard}/></Col>}</Row></section>)}</>;
}

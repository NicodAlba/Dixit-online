import {Row} from "reactstrap";
import useAuthorizedResource from "../hooks/useAuthorizedResource";
import ResourceFeedback from "../components/ui/ResourceFeedback";
import StatCard from "./StatCard";
export default function GlobalStats(){
    const resource=useAuthorizedResource("/api/v1/stats/global"), stats=resource.data;
    if(resource.loading||resource.error)return <ResourceFeedback resource={resource} label="global statistics"/>;
    if(!stats)return <p className="records-feedback">No statistics available yet.</p>;
    const groups=[{title:"Games overview",size:"3",items:[
        ["Total Games Played",stats.totalGamesPlayed??0],["Average Games/User",(stats.averageGamesPerUser??0).toFixed(2)],["Max Games By User",stats.maxGamesByUser??0],["Min Games By User",stats.minGamesByUser??0]]},
        {title:"Community & players",items:[["Total Users Played",stats.totalUsersThatHavePlayed??0],["Current Users Playing",stats.currentUsersPlaying??0],["Avg Players Per Game",(stats.averagePlayersPerGame??0).toFixed(2)],["Max Players In A Game",stats.maxPlayersInGame??0],["Min Players In A Game",stats.minPlayersInGame??0]]},
        {title:"Time records",size:"3",unit:"min",items:[["Total Time Played",stats.totalDuration??0],["Avg Game Duration",(stats.averageDuration??0).toFixed(2)],["Longest Game",stats.maxDuration??0],["Shortest Game",stats.minDuration??0]]}];
    return <>{stats.totalGamesPlayed===0&&<p className="records-feedback">No completed games yet. Your first story will start the record.</p>}{groups.map(group=><section key={group.title}><h2 className="section-title">{group.title}</h2><Row className="justify-content-center">{group.items.map(([title,value])=><StatCard key={title} title={title} value={value} unit={group.unit} colSize={group.size}/>)}</Row></section>)}</>;
}

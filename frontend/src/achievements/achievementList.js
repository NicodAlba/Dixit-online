import React from "react";
import getIdFromUrl from "../util/getIdFromUrl";
import useAuthorizedResource from "../hooks/useAuthorizedResource";
import ResourceFeedback from "../components/ui/ResourceFeedback";
import locked from "../static/images/Achievements/Locked.png";
import "../static/css/records.css";
import "../static/css/Achievements/achievements.css";
function AchievementCard({achievement}) {
    const completed=achievement.dateObtained!=null;
    return <li><article className={`achievement-card ${completed?"achievement-earned":"achievement-locked"}`}>
        <img src={completed?achievement.badgeImage?.url:locked} alt={`${completed?"Earned":"Locked"} achievement: ${achievement.name}`} width="90" height="90"/>
        <div><p className="achievement-status">{completed?"Earned":"Locked"}</p><h3>{achievement.name}</h3><p>{achievement.description}</p>
            <details><summary aria-label={`Details for ${achievement.name}`}>Details</summary><dl><dt>Threshold</dt><dd>{achievement.threshold}</dd>{completed&&<><dt>Date obtained</dt><dd><time dateTime={achievement.dateObtained}>{new Date(achievement.dateObtained).toLocaleDateString("en-GB",{day:"numeric",month:"short",year:"numeric"})}</time></dd></>}</dl></details>
        </div>
    </article></li>;
}
export default function AchievementList(){
    const userId=getIdFromUrl(2);const resource=useAuthorizedResource(userId?`/api/v1/users/${userId}/achievements`:null);
    const achievements=Array.isArray(resource.data)?resource.data:[];
    const groups=[{title:"Completed",items:achievements.filter(a=>a.dateObtained!=null),empty:"You haven't completed any achievement yet."},{title:"Locked",items:achievements.filter(a=>a.dateObtained==null),empty:"You have completed all achievements!"}];
    return <main className="records-page achievements-page"><div className="records-shell"><header className="records-header"><p>Milestones of imagination</p><h1 className="records-title">Achievements</h1><p>Keep telling stories. There is always something to discover.</p></header>
        {resource.loading||resource.error?<ResourceFeedback resource={resource} label="achievements"/>:achievements.length===0?<p className="records-feedback">No achievements available yet.</p>:groups.map(group=><section className="records-panel achievement-section" key={group.title}><h2>{group.title} <span>({group.items.length})</span></h2>{group.items.length===0?<p>{group.empty}</p>:<ul className="achievement-grid">{group.items.map(achievement=><AchievementCard key={achievement.id} achievement={achievement}/>)}</ul>}</section>)}
    </div></main>;
}

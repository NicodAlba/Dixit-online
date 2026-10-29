import React, {createContext, useContext} from "react";
import {FiWifi, FiWifiOff, FiHelpCircle} from "react-icons/fi";
import Tooltip from "../components/ui/Tooltip";
import {usePresenceData} from "./PresenceContext";
import "../static/css/presence.css";

const Participants=createContext(null);
export function ParticipantPresence({gameId,players,children}) {
    const presence=usePresenceData(gameId,true);
    return <Participants.Provider value={presence}>
        {presence.error && <p className="presence-error" role="status">{presence.error} <a href="/login">Log in</a></p>}
        {presence.notices.length>0 && <div className="presence-notices" role="status">
            <ul>{presence.notices.map(notice=>{
                const player=players?.find(player=>player.user?.id===notice.accountId);
                return player ? <li key={notice.key}>{player.user.username}{notice.online?" is back.":" disconnected. Their place is preserved."}</li> : null;
            })}</ul><button onClick={presence.dismiss} aria-label="Dismiss connection notices">×</button>
        </div>}
        {children}
    </Participants.Provider>;
}
export function ConnectionBadge({accountId,presence:provided}) {
    const participants=useContext(Participants);
    const presence=provided||participants;
    if(!presence?.active)return null;
    const online=presence.connections?.[accountId];
    const status=online===true?"online":online===false?"offline":"unknown";
    const label=online===true?"Online":online===false?"Offline":presence.loading?"Checking…":"Unknown";
    const Icon=online===true?FiWifi:online===false?FiWifiOff:FiHelpCircle;
    return <Tooltip className="presence-tooltip" text={online===true?"Connected to Dixit.":online===false?"No active connection to Dixit. Their place is preserved.":presence.loading?"Checking connection information.":"Connection information is unavailable."}>
        <span className={`connection-badge ${status}`} tabIndex={0} aria-label={`Connection status: ${label}`}><Icon aria-hidden="true" /> {label}</span>
    </Tooltip>;
}

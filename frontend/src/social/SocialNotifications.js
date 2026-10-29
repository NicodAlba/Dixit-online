import React from "react";
import {FiMail, FiUserPlus, FiX} from "react-icons/fi";
import {useSocial} from "./SocialContext";
import Tooltip from "../components/ui/Tooltip";

export default function SocialNotifications() {
    const social = useSocial();
    return <div className="social-notifications" aria-live="polite" aria-relevant="additions">
        {social.notifications.length > 0 && <section aria-label="Social notifications">{social.notifications.map(item => <div className="social-notification" key={item.id}>
            {item.section === "invitations" ? <FiMail aria-hidden="true" /> : <FiUserPlus aria-hidden="true" />}
            <p>{item.message}</p>
            <button onClick={() => {social.openPanel(item.section); social.dismissNotification(item.id);}}>View</button>
            <Tooltip text="Dismiss notification"><button aria-label={`Dismiss: ${item.message}`} onClick={() => social.dismissNotification(item.id)}><FiX aria-hidden="true" /></button></Tooltip>
        </div>)}</section>}
    </div>;
}

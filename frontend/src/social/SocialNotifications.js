import React, {useEffect, useState} from "react";
import {FiMail, FiUserPlus, FiMessageCircle, FiX} from "react-icons/fi";
import {useSocial} from "./SocialContext";
import Tooltip from "../components/ui/Tooltip";

function Notice({item, social}) {
    const [hovered, setHovered] = useState(false);
    const [focused, setFocused] = useState(false);
    const paused = hovered || focused;
    const dismiss = social.dismissNotification;
    useEffect(() => {
        if (paused) return;
        const timer = setTimeout(() => dismiss(item.id), 12000);
        return () => clearTimeout(timer);
    }, [item.id, paused, dismiss]);
    return <div className="social-notification" onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}
        onFocusCapture={() => setFocused(true)} onBlurCapture={event => {if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false);}}>
        {item.section === "chat" ? <FiMessageCircle aria-hidden="true" /> : item.section === "invitations" ? <FiMail aria-hidden="true" /> : <FiUserPlus aria-hidden="true" />}
        <p>{item.message}</p>
        <button onClick={() => {if (item.onView) item.onView(); else social.openPanel(item.section); dismiss(item.id);}}>View</button>
        <Tooltip text="Dismiss notification"><button aria-label={`Dismiss: ${item.message}`} onClick={() => dismiss(item.id)}><FiX aria-hidden="true" /></button></Tooltip>
    </div>;
}

export default function SocialNotifications() {
    const social = useSocial();
    return <div className="social-notifications" aria-live="polite" aria-relevant="additions">
        {social.notifications.length > 0 && <section aria-label="Social notifications">{social.notifications.map(item => <Notice key={item.id} item={item} social={social} />)}</section>}
    </div>;
}

import React from "react";
import {Link} from "react-router-dom";
import {FiUsers} from "react-icons/fi";
import Tooltip from "../components/ui/Tooltip";
import {useSocial} from "./SocialContext";

export default function SocialToggle() {
    const social = useSocial();
    return <Tooltip text="Friends">{social
        ? <button id="friends" className="social-toggle" aria-label="Friends" aria-haspopup="dialog" aria-expanded={social.open} aria-controls="social-panel" onClick={social.openPanel}><FiUsers aria-hidden="true" size={22} />{social.relationships.some(item => item.status === "INCOMING") && <span className="social-dot" aria-label="Friend requests waiting" />}</button>
        : <Link id="friends" className="social-toggle" to="/friends" aria-label="Friends"><FiUsers aria-hidden="true" size={22} /></Link>}
    </Tooltip>;
}

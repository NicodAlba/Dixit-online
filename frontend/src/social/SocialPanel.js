import React, {useEffect, useState} from "react";
import {Modal, ModalHeader, ModalBody} from "reactstrap";
import {FiUsers, FiUserPlus, FiMail, FiSearch, FiRefreshCw, FiX} from "react-icons/fi";
import Tooltip from "../components/ui/Tooltip";
import {useSocial} from "./SocialContext";
import {FriendList} from "./FriendList";
import {FriendRequestList} from "./FriendRequestList";
import {SearchUser} from "./SearchUser";
import {GameInvitationList} from "../home/GameInvitationList";
import "../static/css/social/social.css";

export default function SocialPanel() {
    const social = useSocial();
    const [section, setSection] = useState("friends");
    const [inviteRefresh, setInviteRefresh] = useState(0);
    useEffect(() => {
        if (!social.open) return;
        const timer = setInterval(() => setInviteRefresh(value => value + 1), 10000);
        return () => clearInterval(timer);
    }, [social.open]);
    const incoming = social.relationships.filter(item => item.status === "INCOMING").length;
    return <Modal id="social-panel" isOpen={social.open} toggle={social.closePanel} className="social-drawer" modalClassName="social-shell" labelledBy="social-title" returnFocusAfterClose onClosed={() => queueMicrotask(() => {
        if (document.activeElement && document.activeElement !== document.body) return;
        const trigger = document.getElementById("friends");
        if (trigger?.getClientRects().length) trigger.focus();
        else document.querySelector('[aria-controls="main-navigation"]')?.focus();
    })}>
        <ModalHeader close={<Tooltip text="Close"><button aria-label="Close" onClick={social.closePanel}><FiX aria-hidden="true" size={20} /></button></Tooltip>}><span id="social-title"><FiUsers aria-hidden="true" /> Friends</span><span className="social-subtitle">Good stories are better together.</span></ModalHeader>
        <ModalBody>
            <nav className="social-sections" aria-label="Social sections">
                <button aria-pressed={section === "friends"} onClick={() => setSection("friends")}><FiUsers aria-hidden="true" /> Friends</button>
                <button aria-pressed={section === "requests"} onClick={() => setSection("requests")}><FiUserPlus aria-hidden="true" /> Requests{incoming > 0 ? ` (${incoming})` : ""}</button>
                <button aria-pressed={section === "search"} onClick={() => setSection("search")}><FiSearch aria-hidden="true" /> Search</button>
                <button aria-pressed={section === "invitations"} onClick={() => setSection("invitations")}><FiMail aria-hidden="true" /> Invitations</button>
            </nav>
            <div className="social-refresh"><button onClick={() => {social.refresh(); setInviteRefresh(value => value + 1);}} disabled={social.loading || social.pending}><FiRefreshCw aria-hidden="true" /> Refresh</button>{social.loading && <span role="status">{social.loaded ? "Refreshing…" : "Loading friends…"}</span>}</div>
            {social.error && <p className="social-error" role="alert">{social.error}</p>}
            {social.feedback && <p className="social-feedback" role="status">{social.feedback}</p>}
            {section === "friends" && social.loaded && <FriendList />}
            {section === "requests" && social.loaded && <FriendRequestList />}
            {section === "search" && <SearchUser />}
            {section === "invitations" && <section><h3>Game invitations</h3><GameInvitationList refreshToken={inviteRefresh} emptyMessage="No game invitations." /></section>}
        </ModalBody>
    </Modal>;
}

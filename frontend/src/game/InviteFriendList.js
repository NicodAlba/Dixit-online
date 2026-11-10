import React, {useEffect, useRef, useState} from "react";
import {FiMail, FiRefreshCw, FiUsers} from "react-icons/fi";
import {useSocial} from "../social/SocialContext";
import SocialAvatar from "../social/SocialAvatar";
import {ConnectionBadge} from "../presence/ParticipantPresence";
import "../static/css/social/social.css";

export function InviteFriendList({game}) {
    const social = useSocial();
    const presence = social?.presence;
    const [invitations, setInvitations] = useState({});
    const [inviting, setInviting] = useState(null);
    const pending = useRef(null);
    const revision = useRef(0);
    useEffect(() => {
        revision.current++;
        pending.current = null;
        setInviting(null);
        setInvitations({});
        return () => {revision.current++;};
    }, [game?.id]);
    const friends = social?.friends || [];
    const members = new Set((game?.players || []).map(player => player.user?.id));
    const closed = !game?.id || game.startDate || game.endDate || game.cancelled;
    const full = (game?.players?.length || 0) >= 4;

    async function invite(friend) {
        if (pending.current !== null || closed || full || members.has(friend.id) || invitations[friend.id]?.sent || !social) return;
        const scope = revision.current;
        pending.current = friend.id;
        setInviting(friend.id);
        setInvitations(previous => ({...previous, [friend.id]: {}}));
        try {
            await social.request(`/api/v1/games/${game.id}/invite`, "POST", {invited: friend.id});
            if (scope === revision.current) setInvitations(previous => ({...previous, [friend.id]: {sent: true}}));
        } catch (error) {
            if (scope === revision.current) setInvitations(previous => ({...previous, [friend.id]: {error: error.message || "Unable to send invitation. Please try again."}}));
        } finally {
            if (scope === revision.current) {pending.current = null; setInviting(null);}
        }
    }

    return <section className="lobby-friend-panel" aria-labelledby="lobby-friends-title">
        <header className="lobby-friend-header"><h2 id="lobby-friends-title"><FiUsers aria-hidden="true" /> Friends</h2>
            <p>Share this story with a friend.</p></header>
        <div className="lobby-friend-body">
            {!social ? <p className="social-empty">Log in to invite your friends.</p> : <>
                <div className="lobby-friend-toolbar"><span>Invite to lobby #{game?.id}</span>
                    <button type="button" className="lobby-friend-refresh" onClick={social.refresh} disabled={social.loading} aria-label="Refresh friends"><FiRefreshCw aria-hidden="true" /></button></div>
                {social.loading && !social.loaded && <p role="status">Loading friends…</p>}
                {social.error && <p className="social-error" role="alert">{social.error}</p>}
                {full && <p className="social-feedback" role="status">This lobby is full.</p>}
                {closed && <p className="social-feedback" role="status">Invitations are available in an open lobby.</p>}
                {social.loaded && !friends.length && <div className="social-empty"><p>No friends yet. Find someone to share a story with.</p>
                    <button type="button" onClick={() => social.openPanel("search")}>Find friends</button></div>}
                <ul className="social-list lobby-friend-list">{friends.map(friend => {
                    const status = invitations[friend.id] || {};
                    const joined = members.has(friend.id);
                    const label = joined ? "In this lobby" : inviting === friend.id ? "Sending…" : status.sent ? "Invitation sent" : status.error ? "Retry invitation" : "Invite";
                    return <li key={friend.id} className="social-person">
                        <div className="social-person-name"><SocialAvatar username={friend.username} /><div><strong>{friend.username}</strong><ConnectionBadge accountId={friend.id} presence={presence} /></div></div>
                        <div className="social-person-actions"><button type="button" className="social-action-primary" aria-label={`${label}: ${friend.username}`}
                            disabled={closed || full || inviting !== null || status.sent || joined} onClick={() => invite(friend)}><FiMail aria-hidden="true" /> {label}</button></div>
                        {status.sent && !joined && <p className="lobby-invite-result" role="status">Invitation sent to {friend.username}.</p>}
                        {status.error && <p className="social-error" role="alert">{status.error}</p>}
                    </li>;
                })}</ul>
            </>}
        </div>
    </section>;
}

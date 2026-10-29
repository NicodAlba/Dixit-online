import React from "react";
import {useSocial} from "./SocialContext";
import SocialAvatar from "./SocialAvatar";

export function FriendRequestList() {
    const social = useSocial();
    const incoming = social.relationships.filter(item => item.status === "INCOMING");
    const outgoing = social.relationships.filter(item => item.status === "OUTGOING");
    return <section><h3>Friend requests</h3>
        {!incoming.length && <p className="social-empty">No requests waiting for you.</p>}
        <ul className="social-list">{incoming.map(request => <li className="social-person" key={request.requestId}><div className="social-person-name"><SocialAvatar username={request.username} /><strong>{request.username}</strong></div><div className="social-person-actions"><button className="social-action-primary" disabled={social.pending} onClick={() => social.acceptRequest(request.requestId)}>Accept request</button><button disabled={social.pending} onClick={() => social.denyRequest(request.requestId)}>Decline</button></div></li>)}</ul>
        <h3>Sent requests</h3>
        {!outgoing.length && <p className="social-empty">No pending sent requests.</p>}
        <ul className="social-list">{outgoing.map(request => <li className="social-person" key={request.requestId}><div className="social-person-name"><SocialAvatar username={request.username} /><strong>{request.username}</strong></div><span className="social-request-state">Request sent</span></li>)}</ul>
    </section>;
}

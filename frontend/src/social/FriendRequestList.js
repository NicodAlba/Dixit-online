import React from "react";
import {useSocial} from "./SocialContext";

export function FriendRequestList() {
    const social = useSocial();
    const incoming = social.relationships.filter(item => item.status === "INCOMING");
    const outgoing = social.relationships.filter(item => item.status === "OUTGOING");
    return <section><h3>Friend requests</h3>
        {!incoming.length && <p>No requests waiting for you.</p>}
        <ul className="social-list">{incoming.map(request => <li className="social-person" key={request.requestId}><strong>{request.username}</strong><div className="social-person-actions"><button disabled={social.pending} onClick={() => social.acceptRequest(request.requestId)}>Accept request</button><button disabled={social.pending} onClick={() => social.denyRequest(request.requestId)}>Decline</button></div></li>)}</ul>
        <h3>Sent requests</h3>
        {!outgoing.length && <p>No pending sent requests.</p>}
        <ul className="social-list">{outgoing.map(request => <li className="social-person" key={request.requestId}><strong>{request.username}</strong><span>Request sent</span></li>)}</ul>
    </section>;
}

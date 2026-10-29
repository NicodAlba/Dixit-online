import React, {useState} from "react";
import {useNavigate} from "react-router-dom";
import {FiEye, FiMail, FiUserMinus} from "react-icons/fi";
import {usePresenceData} from "../presence/PresenceContext";
import {ConnectionBadge} from "../presence/ParticipantPresence";
import {useSocial} from "./SocialContext";
import SocialAvatar from "./SocialAvatar";

export function FriendList() {
    const social = useSocial();
    const presence = usePresenceData();
    const navigate = useNavigate();
    const [removing, setRemoving] = useState(null);
    const [inviting, setInviting] = useState(null);
    const [lobbies, setLobbies] = useState([]);
    const [selected, setSelected] = useState("");
    async function spectate(friend) {
        const game = friend.spectatableGame;
        const accepted = await social.perform(() => social.request(`/api/v1/games/${game.id}/spectate/${game.playerId}`, "PATCH"), "Spectator access confirmed.");
        if (accepted) navigate(`/games/${game.id}/spectate/${game.playerId}`);
    }
    async function chooseLobby(friend) {
        const loaded = await social.perform(async () => {
            const games = await social.request("/api/v1/games/my");
            setLobbies(games.filter(game => !game.startDate && !game.endDate && !game.cancelled));
            setSelected(""); setInviting(friend.id);
        }, "Choose a lobby to invite your friend.");
        return loaded;
    }
    return <section aria-label="Friends list"><h3>Your friends</h3>
        {presence.error && <p className="presence-error" role="status">{presence.error} <a href="/login">Log in</a></p>}
        {!social.friends.length && <p className="social-empty">No friends yet. Find someone to share a story with.</p>}
        <ul className="social-list">{social.friends.map(friend => <li key={friend.id} className="social-person">
            <div className="social-person-name"><SocialAvatar username={friend.username} /><div><strong>{friend.username}</strong><ConnectionBadge accountId={friend.id} presence={presence} /></div></div>
            <div className="social-person-actions">
                {friend.spectatableGame && <button disabled={social.pending} onClick={() => spectate(friend)}><FiEye aria-hidden="true" /> Spectate</button>}
                <button className="social-action-primary" disabled={social.pending} onClick={() => chooseLobby(friend)}><FiMail aria-hidden="true" /> Invite to lobby</button>
                <button disabled={social.pending} onClick={() => setRemoving(friend.id)}><FiUserMinus aria-hidden="true" /> Remove</button>
            </div>
            {removing === friend.id && <div className="social-confirm"><p>Remove {friend.username} from your friends?</p><button disabled={social.pending} onClick={async () => {if (await social.removeFriend(friend.id)) setRemoving(null);}}>Remove friend</button><button onClick={() => setRemoving(null)}>Cancel</button></div>}
            {inviting === friend.id && <form className="social-invite" onSubmit={async event => {event.preventDefault(); if (selected && await social.perform(() => social.request(`/api/v1/games/${selected}/invite`, "POST", {invited: friend.id}), "Invitation sent.")) setInviting(null);}}>
                {lobbies.length ? <><label htmlFor={`invite-lobby-${friend.id}`}>Choose a lobby</label><select required id={`invite-lobby-${friend.id}`} value={selected} onChange={event => setSelected(event.target.value)}><option value="">Select a lobby</option>{lobbies.map(game => <option key={game.id} value={game.id}>Game {game.id}</option>)}</select><button disabled={!selected || social.pending}>Send invitation</button></> : <p>You have no open lobbies. Create or join a lobby first.</p>}
                <button type="button" onClick={() => setInviting(null)}>Cancel</button>
            </form>}
        </li>)}</ul>
    </section>;
}

import React, {useCallback, useRef, useState} from "react";
import tokenService from "../services/token.service";
import {FiMail, FiCheck} from "react-icons/fi";
import SocialAvatar from "../social/SocialAvatar";
import {useSocial} from "../social/SocialContext";
import useFetchState from "../util/useFetchState";
import "../../src/static/css/social/social.css"
import {useNavigate} from "react-router-dom";

export function GameInvitationList({emptyMessage, refreshToken}) {
    const jwt = tokenService.getLocalAccessToken();
    return jwt ? <AuthenticatedGameInvitationList key={jwt} jwt={jwt} emptyMessage={emptyMessage} refreshToken={refreshToken} /> : null;
}

function AuthenticatedGameInvitationList({jwt, emptyMessage, refreshToken}) {
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const [pendingId, setPendingId] = useState(null);
    const social = useSocial();
    const pending = social ? social.invitationPending !== null : pendingId !== null;
    const pendingRef = useRef(false);

    const navigate = useNavigate()

    const expireSession = useCallback((error, destination = "/") => {
        if (error.status !== 401) return false;
        // A delayed rejection of an old token must not remove a newer login.
        if (tokenService.getLocalAccessToken() !== jwt) return true;
        tokenService.removeUser();
        window.location.replace(destination);
        return true;
    }, [jwt]);

    const [localInvitations, setLocalInvitations, status] = useFetchState(
        [],
        social ? null : "/api/v1/games/invitations",
        jwt,
        setMessage,
        setVisible,
        refreshToken ?? null,
        expireSession
    )

    const invitations = social ? social.invitations : localInvitations;
    const removeInvitation = id => social ? social.removeInvitation(id)
        : setLocalInvitations(previous => previous.filter(item => item.id !== id));
    React.useEffect(() => {
        if (social?.invitationError && !expireSession(social.invitationError)) {
            setMessage(social.invitationError.message); setVisible(true);
        }
    }, [social?.invitationError, expireSession]);

    async function handleAccept(invitation) {
        if (pendingRef.current || (social && !social.beginInvitation(invitation.id))) return;
        pendingRef.current = true;
        setPendingId(invitation.id);
        setVisible(false);
        try {
            const response = await fetch(`/api/v1/games/invitations/${invitation.id}/accept`, {
                method: "PATCH",
                headers: {
                    Authorization: `Bearer ${jwt}`,
                    Accept: "application/json",
                    "Content-Type": "application/json",
                }
            });
            const data = await response.json().catch(() => ({}));
            if (tokenService.getLocalAccessToken() !== jwt) return;
            if (!response.ok) {
                if (response.status === 401 && expireSession({status: 401}, "/login")) return;
                if (response.status === 410) {
                    removeInvitation(invitation.id);
                    setMessage("This invitation is no longer available."); setVisible(true);
                    return;
                }
                const fallback = response.status === 404 ? "Game not found."
                    : response.status === 403 ? "You are not invited to this game."
                    : "Unable to join this game.";
                throw new Error(data.message || fallback);
            }
            removeInvitation(invitation.id);
            navigate(`/games/${invitation.gameId}`);
        } catch (error) {
            setMessage(error.message || "Unable to join this game.");
            setVisible(true);
        } finally {
            pendingRef.current = false;
            setPendingId(null);
            if (social) social.endInvitation();
        }
    }

    return <section className="game-invitations" aria-label="Game invitations">
        {visible && message && <p className="social-error" role="alert">{message}</p>}
        {emptyMessage && (social ? social.loading && !social.loaded : status?.loading) && <p role="status">Loading invitations…</p>}
        {emptyMessage && !(social ? social.loading : status?.loading) && !invitations.length && !visible && <p className="social-empty">{emptyMessage}</p>}
        {invitations.length > 0 && <>
            <h3><FiMail aria-hidden="true" /> Invitations</h3>
            <ul className="social-list">{invitations.map(invitation => <li key={invitation.id} className="social-person invitation-card">
                <div className="social-person-name"><SocialAvatar username={invitation.inviter} /><div>
                    <strong>{invitation.inviter}</strong><span>invited you to <b>Lobby {invitation.gameId}</b></span>
                </div></div>
                <button type="button" className="social-action-primary" disabled={pending} onClick={() => handleAccept(invitation)}>
                    <FiCheck aria-hidden="true" />{(social ? social.invitationPending : pendingId) === invitation.id ? "Accepting…" : "Accept"}
                </button>
            </li>)}</ul>
        </>}
    </section>;
}

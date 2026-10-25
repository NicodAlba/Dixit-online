import React, {useCallback, useRef, useState} from "react";
import tokenService from "../services/token.service";
import {Button, Table} from "reactstrap";
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
    const [pending, setPending] = useState(false);
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

    const [invitations, , status] = useFetchState(
        [],
        "/api/v1/games/invitations",
        jwt,
        setMessage,
        setVisible,
        refreshToken ?? null,
        expireSession
    )

    async function handleAccept(invitation) {
        if (pendingRef.current) return;
        pendingRef.current = true;
        setPending(true);
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
            if (!response.ok) {
                if (response.status === 401 && expireSession({status: 401}, "/login")) return;
                const fallback = response.status === 404 ? "Game not found."
                    : response.status === 403 ? "You are not invited to this game."
                    : "Unable to join this game.";
                throw new Error(data.message || fallback);
            }
            navigate(`/games/${invitation.gameId}`);
        } catch (error) {
            setMessage(error.message || "Unable to join this game.");
            setVisible(true);
        } finally {
            pendingRef.current = false;
            setPending(false);
        }
    }

    const invitationList =
        invitations.map((i) => {
            return (
                <tr key={i.id}>
                    <td className="text-center">{i.inviter}: {i.gameId}</td>
                    <td><Button color="primary" disabled={pending} onClick={() => handleAccept(i)}>Accept</Button></td>
                </tr>
            );
        });

    return (
        <div>
            {visible && message && <div role="alert">{message}</div>}
            {emptyMessage && status?.loading && <p role="status">Loading invitations…</p>}
            {emptyMessage && !status?.loading && !invitations.length && !visible && <p>{emptyMessage}</p>}
            { invitations.length > 0 &&
                <div className="ls-container">
                    <h3 className="text-center">Invitations</h3>
                    <div>
                        <Table aria-label="invitations" className="text-center">
                            <tbody>{invitationList}</tbody>
                        </Table>
                    </div>
                </div>
            }
        </div>
    );

}

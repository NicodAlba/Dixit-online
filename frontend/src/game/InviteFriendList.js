import React, {useRef, useState} from "react";
import tokenService from "../services/token.service";
import {Button, Table} from "reactstrap";
import useFetchState from "../util/useFetchState";
import "../../src/static/css/social/social.css"

export function InviteFriendList(props) {
    const jwt = tokenService.getLocalAccessToken()
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const [feedback, setFeedback] = useState(null);
    const [pending, setPending] = useState(false);
    const pendingRef = useRef(false);

    const [friends] = useFetchState(
        [],
        "/api/v1/friends",
        jwt,
        setMessage,
        setVisible
    )

    const handleInvite = async (user) => {
        if (pendingRef.current) return;
        pendingRef.current = true;
        setPending(true);
        setVisible(false);
        setFeedback(null);
        try{
            const game = props.game;
            const invitationToSend = {
                invited: user.id,
                password: game.password || ""
            }
            const response = await fetch(
                `/api/v1/games/${game.id}/invite`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${jwt}`,
                        Accept: "application/json",
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify(invitationToSend)
                }
            );
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Error HTTP: ${response.status}`);
            } else {
                setFeedback("Invitation sent successfully.");
            }
        } catch (error) {
            setMessage(error.message || "Unable to send invitation.");
            setVisible(true);
        } finally {
            pendingRef.current = false;
            setPending(false);
        }
    }

    const friendList =
        friends.map((u) => {
            return (
                <tr key={u.id}>
                    <td className="text-center">{u.username}</td>
                    <td><Button color="primary" disabled={pending} onClick={() => handleInvite(u)}>Invite</Button></td>
                </tr>
            );
        });

    return (
        <div className="ls-container">
            <h3 className="text-center">Friends</h3>
            {visible && message && <div role="alert">{message}</div>}
            {feedback && <div role="status">{feedback}</div>}
            <div>
                <Table aria-label="friends" className="text-center">
                    <tbody>{friendList}</tbody>
                </Table>
            </div>
        </div>
    );

}

import React, {useState} from "react";
import tokenService from "../services/token.service";
import {Button, Table} from "reactstrap";
import useFetchState from "../util/useFetchState";
import "../../src/static/css/social/social.css"
import {useNavigate} from "react-router-dom";

export function GameInvitationList() {
    const jwt = tokenService.getLocalAccessToken();
    return jwt ? <AuthenticatedGameInvitationList key={jwt} jwt={jwt} /> : null;
}

function AuthenticatedGameInvitationList({jwt}) {
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const [alerts, setAlerts] = useState([])

    const navigate = useNavigate()

    const [invitations, setInvitations] = useFetchState(
        [],
        "/api/v1/games/invitations",
        jwt,
        setMessage,
        setVisible
    )

    function handleAccept(invitation) {
        fetch(`/api/v1/games/invitations/${invitation.id}/accept`, {
            method: "PATCH",
            headers: {
                Authorization: `Bearer ${jwt}`,
                Accept: "application/json",
                "Content-Type": "application/json",
            }
        })
            .then(async (response) => {
                const data = await response.json().catch(() => ({}));
                if (!response.ok) {
                    if (response.status === 409) {
                        setMessage(data.message || "Unable to join this game.");
                        setVisible(true);
                    } else if (response.status === 401) {
                        setMessage("Incorrect password.");
                        setVisible(true);
                    } else if (response.status === 404) {
                        setMessage("Game not found.");
                        setVisible(true);
                    } else if (response.status === 403) {
                        setMessage(data.message || "You are not invited to this game.");
                        setVisible(true);
                    } else {
                        setMessage(data.message || "Error joining the game.");
                        setVisible(true);
                    }
                    throw new Error(data.message);
                }
                navigate(`/games/${invitation.gameId}`);
            })
            .catch((error) => console.error("Join error:", error.message));
    }

    const invitationList =
        invitations.map((i) => {
            return (
                <tbody className="ls" style={{paddingTop:30}}>
                <tr key={i.id} style={{flex:1, alignSelf:"center", gap:20, background:"transparent"}}>
                    <td className="text-center">{i.inviter}: {i.gameId}</td>
                </tr>
                <Button color="primary" onClick={() => {
                    handleAccept(i)
                }}>Accept</Button>
                </tbody>
            );
        });

    return (
        <div>
            {visible && message && <div role="alert">{message}</div>}
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

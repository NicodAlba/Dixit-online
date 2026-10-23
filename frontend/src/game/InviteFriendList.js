import React, {useState} from "react";
import tokenService from "../services/token.service";
import {Button, Table} from "reactstrap";
import useFetchState from "../util/useFetchState";
import "../../src/static/css/social/social.css"

export function InviteFriendList(props) {
    const jwt = tokenService.getLocalAccessToken()
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const [alerts, setAlerts] = useState([])

    const [friends, setFriends] = useFetchState(
        [],
        "/api/v1/friends",
        jwt,
        setMessage,
        setVisible
    )

    const handleInvite = async (user) => {
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
                alert("Request sent successfully!")
            }
        } catch (error) {
            console.error("Error when trying to send request:", error);
            alert(`Request couldn't be sent: ${error.message}`);
        }
    }

    const friendList =
        friends.map((u) => {
            return (
                <tbody className="ls">
                <tr key={u.id} style={{flex:1, alignSelf:"center", gap:20, backgroundColor:"transparent"}}>
                    <td className="text-center">{u.username}</td>
                </tr>
                <Button color="primary" onClick={() => {
                    handleInvite(u)
                }}>Invite</Button>
                </tbody>
            );
        });

    return (
        <div className="ls-container">
            <h3 className="text-center">Friends</h3>
            <div>
                <Table aria-label="friends" className="text-center">
                    <tbody>{friendList}</tbody>
                </Table>
            </div>
        </div>
    );

}

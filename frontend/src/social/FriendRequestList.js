import React, {useState} from "react";
import tokenService from "../services/token.service";
import {Button, Table} from "reactstrap";
import useFetchState from "../util/useFetchState";
import "../../src/static/css/social/social.css";
import deleteFromList from "../util/deleteFromList";

export function FriendRequestList() {
    const jwt = tokenService.getLocalAccessToken()
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const [alerts, setAlerts] = useState([])

    const [requests, setRequests] = useFetchState(
        [],
        "api/v1/friends/pending",
        jwt,
        setMessage,
        setVisible
    )

    const handleAcceptRequest = async (request) => {
        fetch(
            `/api/v1/friends/${request.id}/accept`,
            {
                method:  "PATCH",
                headers: {
                    Authorization: `Bearer ${jwt}`,
                    Accept: "application/json",
                    "Content-Type": "application/json",
                }
            }
        ).then((response) => response.json())
            .then((data) => {
                window.location.reload()
                if (data.message) {
                    setMessage(data.message)
                    setVisible(true)
                }
            }).catch((error) => alert(error))
    }

    const handleDenyRequest = async (request) => {
        deleteFromList(
            `/api/v1/friends/${request.id}/deny`,
            request.id,
            [requests, setRequests],
            [alerts, setAlerts],
            setMessage,
            setVisible
        )
    }

    const requestList =
        requests.map((r) => {
            return (
                <tbody className="ls" style={{paddingTop:30}}>
                    <tr key={r.id} style={{flex:1, alignSelf:"center", backgroundColor:"transparent"}}>
                        <td className="text-center">{r.sender.username}</td>
                    </tr>
                        <Button color="success" onClick={() => {
                        handleAcceptRequest(r)
                    }}>Accept</Button>
                        <Button color="danger" onClick={() => {
                            handleDenyRequest(r)
                        }}>Deny</Button>
                </tbody>
            );
        });

    return (
        <div className="ls-container">
            <h3 className="text-center">Friend requests</h3>
            <div>
                <Table aria-label="friend requests" className="text-center">
                    <tbody>{requests.length >= 1 ? requestList : "You have no pending requests"}</tbody>
                </Table>
            </div>
        </div>
    );

}

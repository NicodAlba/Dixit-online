import React, {useState} from "react";
import tokenService from "../services/token.service";
import {Button, Table} from "reactstrap";
import useFetchState from "../util/useFetchState";
import "../../src/static/css/social/social.css"
import deleteFromList from "../util/deleteFromList";
import {useNavigate} from 'react-router-dom';

export function FriendList() {
    const jwt = tokenService.getLocalAccessToken()
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const [alerts, setAlerts] = useState([])

    const [friends, setFriends] = useFetchState(
        [],
        "http://localhost:3000/api/v1/friends",
        jwt,
        setMessage,
        setVisible
        )
    
    const navigate = useNavigate();
    const user = tokenService.getUser();

    const [fetchedUser, setFetchedUser] = useFetchState(user, `/api/v1/users/${user?.id}`, jwt, setMessage, setVisible)

    const handleRemoveFriend = async (user) => {
        deleteFromList(
            `/api/v1/friends/${user.id}/remove`,
            user.id,
            [friends, setFriends],
            [alerts, setAlerts],
            setMessage,
            setVisible
        )
    }

    const handleSpectate = async (lastGame, player) => {
        try{
            const response = await fetch(
                `/api/v1/games/${lastGame.id}/spectate/${player.id}`,
                {
                    method: "PATCH",
                    headers: {
                        Authorization: `Bearer ${jwt}`,
                        Accept: "application/json",
                    },
                }
            );

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Error HTTP: ${response.status}`);
            }

            navigate(`/games/${lastGame.id}/spectate/${player.id}`);
        } catch (error){
            console.error("Error when trying to spectate", error);
            alert(`You cannot spectate this match: ${error.message}`);
        }
    };

    const gamesToSpectate = fetchedUser?.gamesToSpectate
    console.log(gamesToSpectate)
    const friendList =
        friends.map((u) => {
            const lastPlayer = u.players[u.players.length - 1]
            console.log(lastPlayer)
            const gameToSpectate = gamesToSpectate?.find((game) => game?.players.some((p) => p.id === lastPlayer.id))
            console.log(gameToSpectate)
            return (
                <tbody className="ls" style={{paddingTop:30}}>
                    <tr key={u.id} style={{flex:1, alignSelf:"center", backgroundColor:"transparent"}}>
                        <td className="text-center">{u.username}</td>
                    </tr>
                    {gameToSpectate && <Button color="warning" onClick={() => {
                        handleSpectate(gameToSpectate, lastPlayer)
                    }}>Spectate</Button>}
                    <Button color="danger" onClick={() => {
                        handleRemoveFriend(u)
                    }}>Remove</Button>
                </tbody>
            );
        });

    return (
        <div className="ls-container">
            <h3 className="text-center">Friends</h3>
            <div>
                <Table aria-label="friends" className="text-center">
                    <tbody>{friends.length >= 1 ? friendList : "You have no friends"}</tbody>
                </Table>
            </div>
        </div>
    );

}

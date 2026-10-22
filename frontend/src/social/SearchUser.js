import React, {useEffect, useState} from "react"
import tokenService from "../services/token.service";
import {Button, Input, Table} from "reactstrap";
import "../../src/static/css/social/social.css"
import getErrorModal from "../util/getErrorModal";

const jwt = tokenService.getLocalAccessToken();

export function SearchUser() {
    const [message, setMessage] = useState(null);
    const [visible, setVisible] = useState(false);
    const [searchText, setSearchText] = useState("");
    const [page, setPage] = useState(0);
    const [users, setUsers] = useState([])

    const fetchPlayers = async () => {
        try{
            const response = await fetch(
                `api/v1/users/players?page=${page}&size=5&username=${searchText}`,
                {
                    method: "GET",
                    headers: {
                        Authorization: `Bearer ${jwt}`,
                        Accept: "application/json",
                        "Content-Type": "application/json",
                    }
                }
            );
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Error HTTP: ${response.status}`);
            }
            const usersData = await response.json();
            setUsers(usersData.content)
        } catch (error){
            console.error("Error when searching users:", error);
            alert(`Users couldn't be found: ${error.message}`);
        }
    }

    const handleSendRequest = async (user) => {
        try{
            const response = await fetch(
                `api/v1/friends/addFriend/${user.id}`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${jwt}`,
                        Accept: "application/json",
                        "Content-Type": "application/json",
                    }
                }
            );
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || `Error HTTP: ${response.status}`);
            } else {
                alert("Request sent successfully!")
                setUsers(users.filter((u) => u.id !== user.id))
            }
        } catch (error) {
            console.error("Error when trying to send request:", error);
            alert(`Request couldn't be sent: ${error.message}`);
        }
    }

    const userList =
        users.map((u) => {
            return (
                <div>
                    <Table>
                        <tbody className="ls">
                        <tr key={u.id} style={{flex:1, alignSelf:"center", backgroundColor:"transparent"}}>
                            <td className="text-center" style={{background:"transparent"}}>{u.username}</td>
                        </tr>
                        <Button size="sm" onClick={() => {
                            handleSendRequest(u)
                        }}>Send</Button>
                        </tbody>
                    </Table>
                </div>
            );
    });

    function handleChange(event) {
        const target = event.target
        const value = target.value
        setSearchText(value || " ")
    }

    return (
        <div className="ls-container">
            <h3 className="text-center">Add friends</h3>
            <div className="search-bar">
                <Input name="searchText" id="searchText" placeholder="Search.." style={{height:32.5}}
                       value={searchText}
                       onChange={handleChange}/>
                <Button className= "align-middle" style={{height:32.5}} color="warning" size="sm" onClick={fetchPlayers}>Done</Button>
            </div>
            <div>
                {userList}
            </div>
        </div>
    );


}

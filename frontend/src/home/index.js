import React, { useState, useEffect } from 'react';
import '../App.css';
import '../static/css/home/home.css'
import logo from '../static/images/Logo dixit.webp'
import tokenService from "../services/token.service"
import jwt_decode from "jwt-decode";
import { useNavigate } from "react-router-dom"
import { Button, Form, Input, Label } from 'reactstrap';
import { GameInvitationList } from "./GameInvitationList";


export default function Home() {
    const jwt = tokenService.getLocalAccessToken()
    const [roles, setRoles] = useState([]);

    useEffect(() => {
        if (jwt) {
            setRoles(jwt_decode(jwt).authorities);
        }
    }, [jwt])
    // TODO: fix
    const emptyGame = {
        password: ""
    }

    const [showOptions, setShowOptions] = useState(false)
    const [view, setView] = useState(null);

    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)

    const [game, setGame] = useState(emptyGame)

    const navigate = useNavigate()

    function handleSubmitCreate(event) {
        event.preventDefault()
        const gameToSend = { password: game.password || "" };
        fetch(
            "/api/v1/games",
            {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${jwt}`,
                    Accept: "application/json",
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(gameToSend),
            }
        ).then((response) => response.json())
            .then((data) => {
                if (data.message) {
                    setMessage(data.message)
                    setVisible(true)
                } else {
                    const id = data.id
                    navigate(`/games/${id}`)
                }
            }).catch((error) => alert(error))
    }

    function handleSubmitJoin(event) {
        event.preventDefault();
        const passwordToSend = game.password || "";
        const gameId = game.gameId;

        fetch(`/api/v1/games/${gameId}/join`, {
            method: "PATCH",
            headers: {
                Authorization: `Bearer ${jwt}`,
                Accept: "application/json",
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ password: passwordToSend }),
        })
            .then(async (response) => {
                const data = await response.json().catch(() => ({}));
                if (!response.ok) {
                    if (response.status === 409) {
                        setMessage("You are already in this game.");
                        setVisible(true);
                        navigate("/");
                    } else if (response.status === 401) {
                        setMessage("Incorrect password.");
                        setVisible(true);
                    } else if (response.status === 404) {
                        setMessage("Game not found.");
                        setVisible(true);
                    } else if (response.status === 403) {
                        setMessage("Already 4 players in the game");
                        setVisible(true);
                    } else {
                        setMessage(data.message || "Error joining the game.");
                        setVisible(true);
                    }
                    throw new Error(data.message);
                }
                const updatedGame = data;
                navigate(`/games/${updatedGame.id}`);
            })
            .catch((error) => console.error("Join error:", error.message));
    }


    function handleChange(event) {
        const target = event.target
        const value = target.value
        const name = target.name
        setGame({ ...game, [name]: value })
    }

    const handlePlayClick = () => {
        if (!jwt) {
            navigate("/login");
            return;
        }
        setShowOptions(true);

    }

    function renderCreationForm() {
        return (
            <Form onSubmit={handleSubmitCreate}>
                <Label for="password" className="text-center">
                    Add a password to the game.
                </Label>
                <Input
                    type="text"
                    name="password"
                    id="password"
                    placeholder="Enter password"
                    value={game.password || ""}
                    onChange={handleChange}
                    className="custom-input"
                />
                <div className="custom-button-row">
                    <Button color="success" type="submit">
                        Create game
                    </Button>
                </div>
            </Form>
        );
    }
    function renderJoinForm() {
        return (
            <Form onSubmit={handleSubmitJoin}>
                <Label htmlFor="gameId" className="text-center">
                    Enter the game ID:
                </Label>
                <Input
                    type="number"
                    name="gameId"
                    id="gameId"
                    placeholder="Enter game ID"
                    value={game.gameId || ""}
                    onChange={handleChange}
                    className="custom-input"
                />

                <Label htmlFor="password" className="text-center">
                    Enter game password.
                </Label>
                <Input
                    type="text"
                    name="password"
                    id="password"
                    placeholder="Enter password"
                    value={game.password || ""}
                    onChange={handleChange}
                    className="custom-input"
                />
                {visible && message &&
                    <div className="text-danger text-center mt-2">
                        {message}
                    </div>
                }


                <div className="custom-button-row">
                    <Button color="success" type="submit">
                        Join game
                    </Button>
                </div>
            </Form>
        );
    }

    return (
        <div className="home-page-container">
            <div className="hero-div">
                <img src={logo} alt="logo" />
                {roles.includes("ADMIN") ? (
                    <h3>Welcome to the admin page</h3>
                ) : (
                    <>
                        <h3>Do you want to play?</h3>
                        {!showOptions && <Button color="warning" onClick={handlePlayClick}>Play game</Button>}
                        {showOptions && !view && (
                            <div className="container">
                                <div className='custom-button-row'>
                                    <Button
                                        color="warning" onClick={() => setView("create")}
                                    > Create game
                                    </Button>
                                </div>
                                <div className='custom-button-row'>
                                    <Button
                                        color="warning" onClick={() => setView("join")}
                                    > Join game
                                    </Button>
                                </div>
                            </div>
                        )}
                        {view === "create" && renderCreationForm()}
                        {view === "join" && renderJoinForm()}
                    </>
                )}
            </div>
            <GameInvitationList />
        </div>
    );
}

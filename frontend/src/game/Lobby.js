import React, {useEffect, useState} from "react";
import "../static/css/lobby/Lobby.css";
import {useNavigate, useParams} from 'react-router-dom';
import tokenService from "../services/token.service";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import {InviteFriendList} from "./InviteFriendList";

export default function Lobby() {
    const { gameId } = useParams();
    const [gameData, setGameData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showLeaveModal, setShowLeaveModal] = useState(false);

    const jwt = tokenService.getLocalAccessToken();

    const navigate = useNavigate();


    useEffect(() => {
        let cancelled = false;
        let revision = 0;
        if (!gameId) {
            setError("No se encontró el ID de la partida en la URL.");
            setLoading(false);
            return;
        }
        async function fetchGame() {
            const requestRevision = ++revision;
            try {
                const response = await fetch(
                    `/api/v1/games/${gameId}`,
                    {
                        method: "GET",
                        headers: {
                            Authorization: `Bearer ${jwt}`,
                            Accept: "application/json",
                        },
                    }
                );
                if (!response.ok) {
                    throw new Error(`Error HTTP: ${response.status}`);
                }

                const data = await response.json();
                if (cancelled || requestRevision !== revision) return;
                setGameData(data);

                if(data.startDate) {
                    // We change to the lobby if the game started
                    navigate(`/games/${gameId}/game`);
                    return;
                }
            } catch (error) {
                if (cancelled || requestRevision !== revision) return;
                console.error("Error al obtener los datos de la partida:", error);
                setError(error.message);
            } finally {
                if (!cancelled && requestRevision === revision) setLoading(false);
            }
        }
        fetchGame();

        const stompClient = new Client({
            webSocketFactory: () => new SockJS("/ws"),
            reconnectDelay: 5000,
            onConnect: () => {
                if (cancelled) return;
                stompClient.subscribe(`/topic/game/${gameId}`, (message) => {
                    if (cancelled) return;
                    revision++;
                    if (message.body === '"DELETED"') {
                        alert("The leader deleted the game");
                        navigate("/");
                    } else {
                        const updatedGame = JSON.parse(message.body);
                        setGameData(updatedGame);
                        setLoading(false);

                        if(updatedGame.startDate) {
                            navigate(`/games/${gameId}/game`);
                        }
                    }
                });
                // Recover changes made between the initial GET and subscription.
                fetchGame();
            },
            onStompError: (frame) => {
                console.error('Error en WebSocket:', frame.headers['message']);
            }
        });

        stompClient.activate();

        return () => {
            cancelled = true;
            revision++;
            stompClient.deactivate();
        };

    }, [gameId, jwt, navigate]);

    const maxPlayers = 4;
    if (loading) return <div>Cargando partida...</div>;
    if (error) return <div>Error: {error}</div>;
    if (!gameData) return <div>No se encontraron datos de la partida.</div>;

    const players = gameData.players || [];
    const playerCount = players.length;
    const enoughPlayers = playerCount >= 3;

    const user = tokenService.getUser();
    const currentUsername = user?.username ?? null;
    const isLeader = gameData.leader?.username === currentUsername;

    console.log(gameData)
    const handleLeave = () => {
        setShowLeaveModal(true);
    };

    const confirmLeave = async () => {
        try {
            const response = await fetch(
                `/api/v1/games/${gameId}/leave`,
                {
                    method: "PATCH",
                    headers: {
                        Authorization: `Bearer ${jwt}`,
                        Accept: "application/json",
                    },
                }
            );

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `Error HTTP: ${response.status}`);
            }
            navigate("/");
        } catch (error) {
            console.error("Error al salir de la partida:", error);
            alert(`No se pudo salir de la partida: ${error.message}`);
            setShowLeaveModal(false);
        }
    };

    const cancelLeave = () => {
        setShowLeaveModal(false);
    };

    const handleStart = async () => {
        if (!enoughPlayers) return

        try{
            const response = await fetch(
                `/api/v1/games/${gameId}/start`,
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
            const updatedGameData = await response.json();
            setGameData(updatedGameData);
            navigate(`/games/${gameId}/game`);
        } catch (error){
            console.error("Error al iniciar la partida:", error);
            alert(`No se pudo iniciar la partida: ${error.message}`);
        }
    };

    return (
        <div className="lobby-page-container">
            {showLeaveModal && (
                <div className="modal-overlay">
                    <div className="modal-box">
                        <h3 className="modal-text">Are you sure you are leaving the game?</h3>

                        {isLeader && (
                            <p className="modal-warning">
                                You are the leader, if you leave the lobby, it will be deleted.
                            </p>
                        )}

                        <div className="modal-buttons">
                            <button className="btn-cancel" onClick={cancelLeave}>Cancel</button>
                            <button className="btn-confirm" onClick={confirmLeave}>
                                Yes, leave
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div className="lobby-lobby-card">
                <h2 className="lobby-lobby-title">Lobby</h2>

                <div className="lobby-info-id">ID: <span>{gameData.id}</span></div>
                <div
                    className={`lobby-player-count ${
                        enoughPlayers ? "enough" : "not-enough"
                    }`}
                >
                    {playerCount} / {maxPlayers}
                </div>

                <ul className="lobby-player-list">
                    <li key={0} className="lobby-player-item">
                        {gameData.leader?.username || "Jugador desconocido"}
                        <span className="leader-indicator">(L)</span>
                    </li>
                    {players.slice(1).map((p, index) => (
                        <li key={index + 1} className="lobby-player-item">
                            {p.user?.username || "Jugador desconocido"}
                        </li>
                    ))}
                    {Array.from({ length: maxPlayers - playerCount }).map((_, i) => (
                        <li key={`empty-${i}`} className="lobby-player-empty">
                            Waiting for player...
                        </li>
                    ))}
                </ul>

                <div className="lobby-button-container" style={{ justifyContent: isLeader ? "space-between" : "center" }}>
                    <button className="lobby-btn leave" onClick={handleLeave}>
                        Leave
                    </button>
                    {isLeader && (
                        <button
                            className={`lobby-btn start ${enoughPlayers ? "enough" : "not-enough"}`}
                            disabled={!enoughPlayers}
                            onClick={handleStart}
                        >
                            Start
                        </button>
                    )}
                </div>
            </div>
            <div>
                <InviteFriendList game={gameData}/>
            </div>
        </div>
    );
}

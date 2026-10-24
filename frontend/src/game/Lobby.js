import React, {useEffect, useRef, useState} from "react";
import "../static/css/lobby/Lobby.css";
import {useNavigate, useParams} from 'react-router-dom';
import tokenService from "../services/token.service";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import {Modal, ModalBody, ModalFooter, ModalHeader} from "reactstrap";
import {InviteFriendList} from "./InviteFriendList";

export default function Lobby() {
    const { gameId } = useParams();
    const [gameData, setGameData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showLeaveModal, setShowLeaveModal] = useState(false);
    const [pending, setPending] = useState(false);
    const pendingRef = useRef(false);
    const [actionError, setActionError] = useState(null);
    const [connection, setConnection] = useState("connecting");
    const reloadRef = useRef(() => {});

    const jwt = tokenService.getLocalAccessToken();

    const navigate = useNavigate();


    useEffect(() => {
        let cancelled = false;
        let revision = 0;
        setLoading(true);
        setError(null);
        setConnection("connecting");
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
                setError(null);

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
        reloadRef.current = fetchGame;
        fetchGame();

        const stompClient = new Client({
            webSocketFactory: () => new SockJS("/ws"),
            connectHeaders: {Authorization: `Bearer ${jwt}`},
            reconnectDelay: 5000,
            onConnect: () => {
                if (cancelled) return;
                setConnection("connected");
                stompClient.subscribe(`/topic/game/${gameId}`, (message) => {
                    if (cancelled) return;
                    try {
                        const event = JSON.parse(message.body);
                        if (event.type === "DELETED" && event.gameId === Number(gameId)) {
                            revision++;
                            alert("The leader deleted the game");
                            navigate("/");
                        } else if (event.type === "GAME_UPDATED" && event.gameId === Number(gameId)) {
                            fetchGame();
                        }
                    } catch (error) {
                        setError("Invalid lobby notification.");
                    }
                });
                // Recover changes made between the initial GET and subscription.
                fetchGame();
            },
            onWebSocketClose: () => {if (!cancelled) setConnection("reconnecting");},
            onStompError: () => {
                if (!cancelled) {setConnection("error"); setActionError("Unable to connect to this lobby. Check your session.");}
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
    if (loading || !gameData || error) return <main className="lobby-page-container">
        <div className="lobby-lobby-card">
            <h1>Lobby</h1>
            {loading ? <p role="status">Loading game…</p> : <p role="alert">{error || "Game not found."}</p>}
            <button className="lobby-btn leave" onClick={() => navigate("/")}>Back home</button>
            {!loading && <button className="lobby-btn start enough" onClick={() => reloadRef.current()}>Retry</button>}
        </div>
    </main>;

    const players = gameData.players || [];
    const playerCount = players.length;
    const enoughPlayers = playerCount >= 3;

    const user = tokenService.getUser();
    const currentUsername = user?.username ?? null;
    const isLeader = gameData.leader?.username === currentUsername;

    const handleLeave = () => {
        setShowLeaveModal(true);
    };

    const confirmLeave = async () => {
        if (pendingRef.current) return;
        pendingRef.current = true; setPending(true); setActionError(null);
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
            setActionError(error.message || "Unable to leave this lobby.");
            setShowLeaveModal(false);
        } finally {pendingRef.current = false; setPending(false);}
    };

    const cancelLeave = () => {
        setShowLeaveModal(false);
    };

    const handleStart = async () => {
        if (!enoughPlayers || pendingRef.current) return
        pendingRef.current = true; setPending(true); setActionError(null);

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
            setActionError(error.message || "Unable to start this game.");
        } finally {pendingRef.current = false; setPending(false);}
    };

    return (
        <div className="lobby-page-container">
            <Modal isOpen={showLeaveModal} toggle={pending ? undefined : cancelLeave} labelledBy="leave-lobby-title" returnFocusAfterClose>
                <ModalHeader id="leave-lobby-title">Leave this lobby?</ModalHeader>
                <ModalBody>
                    {isLeader ? "You are the host. Leaving deletes this lobby for everyone." : "You can return while there is space in the lobby."}
                </ModalBody>
                <ModalFooter>
                    <button className="btn-cancel" onClick={cancelLeave} disabled={pending}>Cancel</button>
                    <button className="btn-confirm" onClick={confirmLeave} disabled={pending}>{pending ? "Leaving…" : "Yes, leave"}</button>
                </ModalFooter>
            </Modal>

            <div className="lobby-lobby-card">
                <h1 className="lobby-lobby-title">Lobby</h1>
                {connection !== "connected" && <p className="lobby-status" role="status">{connection === "reconnecting" ? "Reconnecting… The lobby will refresh automatically." : connection === "error" ? "Connection unavailable." : "Connecting…"}</p>}
                {actionError && <p className="lobby-error" role="alert">{actionError}</p>}
                {pending && <p role="status">Please wait…</p>}

                <div className="lobby-info-id">ID: <span>{gameData.id}</span></div>
                <div
                    className={`lobby-player-count ${
                        enoughPlayers ? "enough" : "not-enough"
                    }`}
                >
                    {playerCount} / {maxPlayers}
                </div>

                <ul className="lobby-player-list">
                    {players.map((player, index) => (
                        <li key={player.id ?? index} className="lobby-player-item">
                            {player.user?.username || "Unknown player"}
                            {player.user?.username === gameData.leader?.username && <span className="leader-indicator">Host</span>}
                        </li>
                    ))}
                    {Array.from({ length: Math.max(0, maxPlayers - playerCount) }).map((_, i) => (
                        <li key={`empty-${i}`} className="lobby-player-empty">
                            Waiting for player...
                        </li>
                    ))}
                </ul>

                <p className="lobby-help">{enoughPlayers ? isLeader ? "Ready to start. Everyone will enter the game together." : "Ready. Waiting for the host to start." : "At least three players are needed to start."}</p>
                <div className="lobby-button-container" style={{ justifyContent: isLeader ? "space-between" : "center" }}>
                    <button className="lobby-btn leave" disabled={pending} onClick={handleLeave}>
                        Leave
                    </button>
                    {isLeader && (
                        <button
                            className={`lobby-btn start ${enoughPlayers ? "enough" : "not-enough"}`}
                            disabled={!enoughPlayers || pending}
                            onClick={handleStart}
                        >
                            Start
                        </button>
                    )}
                </div>
            </div>
            <div className="lobby-friends">
                <InviteFriendList game={gameData}/>
            </div>
        </div>
    );
}

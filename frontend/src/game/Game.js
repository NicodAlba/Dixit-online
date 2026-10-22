import React, { useEffect, useMemo, useState } from "react";
import "../static/css/game/Game.css";
import { useNavigate } from "react-router-dom";
import tokenService from "../services/token.service";
import getIdFromUrl from "./../util/getIdFromUrl";
import useGameTurnState from "../hooks/useGameTurnState";
import usePlayerNames from "../hooks/usePlayerNames";
import { OwnPlayerPanel } from "./components/OwnPlayerPanel";
import LeftPlayerPanel from "./components/LeftPlayerPanel";
import RightPlayerPanel from "./components/RightPlayerPanel";
import TopPlayerPanel from "./components/TopPlayerPanel";
import TurnStatusPanel from "./components/TurnStatusPanel";
import Chat from "./components/Chat";
import { Button } from "reactstrap";

const jwt = tokenService.getLocalAccessToken();
const currentUser = tokenService.getUser();

export default function Game() {
    const [_message, setMessage] = useState(null);
    const [_visible, setVisible] = useState(false);
    const [pendingPlayedCardIds, setPendingPlayedCardIds] = useState([]);
    const gameId = getIdFromUrl(2);
    const { game, turn, turnId, setTurn, players, storyteller } = useGameTurnState({
        gameId,
        jwt,
        setMessage,
        setVisible,
    });
    const navigate = useNavigate();

    const usernamesById = usePlayerNames(players, jwt);

    const getPlayerName = (player) => {
        if (!player) {
            return "";
        }
        const playerUser = player.user;
        if (playerUser?.username) {
            return playerUser.username;
        }
        if (typeof playerUser === "string") {
            return playerUser;
        }
        const userId = typeof playerUser === "number" ? playerUser : playerUser?.id;
        if (userId != null) {
            return usernamesById[userId] || `Player ${userId}`;
        }
        if (player.username) {
            return player.username;
        }
        return "";
    };

    const currentPlayer =
        players.find((player) => {
            const playerUser = player.user;
            if (playerUser?.id) {
                return playerUser.id === currentUser?.id;
            }
            return playerUser === currentUser?.id;
        }) || { id: null, cards: [], points: 0, user: { username: currentUser?.username || "" } };

    const isStoryteller = storyteller?.id === currentPlayer?.id;

    const playedCardIds = useMemo(() => {
        const ids = new Set(pendingPlayedCardIds);
        (game.turns || []).forEach((gameTurn) => {
            (gameTurn?.playedCards || []).forEach((card) => {
                if (card?.id != null) {
                    ids.add(card.id);
                }
            });
        });
        (turn?.playedCards || []).forEach((card) => {
            if (card?.id != null) {
                ids.add(card.id);
            }
        });
        return ids;
    }, [game.turns, turn?.playedCards, pendingPlayedCardIds]);

    useEffect(() => {
        if (!turn?.playedCards) {
            return;
        }
        setPendingPlayedCardIds((prev) =>
            prev.filter((id) => !turn.playedCards.some((card) => card?.id === id))
        );
    }, [turn?.playedCards]);

    useEffect(() => {
        // Drop pending ids as soon as the player's hand no longer contains them
        const handIds = new Set((currentPlayer?.cards || []).map((card) => card?.id));
        setPendingPlayedCardIds((prev) => prev.filter((id) => !handIds.has(id)));
    }, [currentPlayer?.cards]);

    const getPlayerHandCards = (player) => {
        const cards = player?.cards || [];
        if (!cards.length) {
            return [];
        }
        return cards.filter((card) => card?.id == null || !playedCardIds.has(card.id));
    };

    const refreshTurn = async () => {
        if (!turnId) {
            return;
        }
        try {
            const response = await fetch(`/api/v1/turns/${turnId}`, {
                headers: {
                    Authorization: `Bearer ${jwt}`,
                    Accept: "application/json",
                },
            });
            if (response.ok) {
                const freshTurn = await response.json();
                setTurn(freshTurn);
            }
        } catch (err) {
            console.error("Error refreshing turn", err);
        }
    };

    const selectCard = async (card) => {
        if (!turnId || !currentPlayer?.id) {
            console.warn("Cannot select card without a valid turn or player");
            return;
        }
        const endpoint = isStoryteller
            ? `/api/v1/turns/${turnId}/answer/${card.id}`
            : `/api/v1/turns/${turnId}/play-card/${card.id}/${currentPlayer.id}`;
        const method = isStoryteller ? "PUT" : "POST";
        try {
            setPendingPlayedCardIds((prev) =>
                prev.includes(card.id) ? prev : [...prev, card.id]
            );
            const response = await fetch(endpoint, {
                method,
                headers: {
                    Authorization: `Bearer ${jwt}`,
                    Accept: "application/json",
                    "Content-Type": "application/json",
                },
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || `Error HTTP: ${response.status}`);
            }
            if (payload && payload.state) {
                setTurn(payload);
            } else {
                await refreshTurn();
            }
        } catch (error) {
            console.error("Error when trying to select card:", error);
            alert(`Card couldn't be selected: ${error.message}`);
            setPendingPlayedCardIds((prev) => prev.filter((id) => id !== card.id));
        }
    };

    function handleChange(event) {
        const target = event.target;
        const value = target.value;
        const name = target.name;
        setTurn({ ...turn, [name]: value });
    }

    function handleSendClue(event) {
        event.preventDefault();
        if (!turnId) {
            return;
        }
        const clueToSend = turn?.clue || "";
        fetch(`/api/v1/turns/${turnId}/clue`, {
            method: "PUT",
            headers: {
                Authorization: `Bearer ${jwt}`,
                Accept: "application/json",
                "Content-Type": "application/json",
            },
            body: clueToSend,
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
            })
            .catch((error) => console.error("Clue not valid", error.message));
    }

    const voteCard = async (card) => {
        if (!turnId || !currentPlayer?.id) {
            console.warn("Cannot select card without a valid turn or player");
            return;
        }
        const endpoint = `/api/v1/turns/${turnId}/vote/${currentPlayer?.id}/${card.id}`;
        try {
            const response = await fetch(endpoint, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${jwt}`,
                    Accept: "application/json",
                    "Content-Type": "application/json",
                },
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || `Error HTTP: ${response.status}`);
            }
            if (payload && payload.state) {
                setTurn(payload);
            } else {
                await refreshTurn();
            }
        } catch (error) {
            console.error("Error when trying to select card:", error);
            alert(`Card couldn't be selected: ${error.message}`);
        }
    };

    return (
        <div className="game-page-container">
            <Button  className= "leave-game" style={{position: "absolute", top: 10, left:30}} onClick={() => navigate("/")}>Leave game</Button>
            <TurnStatusPanel
                game={game}
                turn={turn}
                currentPlayer={currentPlayer}
                isStoryteller={isStoryteller}
                storyteller={storyteller}
                handleChange={handleChange}
                handleSendClue={handleSendClue}
                voteCard={voteCard}
                getPlayerName={getPlayerName}
            />
            <OwnPlayerPanel
                currentPlayer={currentPlayer}
                currentUser={currentUser}
                isStoryteller={isStoryteller}
                turn={turn}
                selectCard={selectCard}
                getPlayerName={getPlayerName}
                handCards={getPlayerHandCards(currentPlayer)}
            />
            <LeftPlayerPanel
                currentPlayer={currentPlayer}
                players={players}
                getPlayerName={getPlayerName}
                storyteller={storyteller}
                getPlayerHandCards={getPlayerHandCards}
            />
            <RightPlayerPanel
                currentPlayer={currentPlayer}
                players={players}
                getPlayerName={getPlayerName}
                storyteller={storyteller}
                getPlayerHandCards={getPlayerHandCards}
            />
            <TopPlayerPanel
                currentPlayer={currentPlayer}
                players={players}
                getPlayerName={getPlayerName}
                storyteller={storyteller}
                getPlayerHandCards={getPlayerHandCards}
            />
            <Chat gameId={gameId} />
        </div>
    );
}

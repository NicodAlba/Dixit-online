import React, { useEffect, useMemo, useRef, useState } from "react";
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
import GameLayout from "./components/GameLayout";

export default function Game() {
    const jwt = tokenService.getLocalAccessToken();
    const currentUser = tokenService.getUser();
    const [action, setAction] = useState(null);
    const actionRef = useRef(false);
    const [message, setMessage] = useState(null);
    const [visible, setVisible] = useState(false);
    const [pendingPlayedCardIds, setPendingPlayedCardIds] = useState([]);
    const [clueDraft, setClueDraft] = useState("");
    const [clueError, setClueError] = useState(null);
    const gameId = getIdFromUrl(2);
    const { game, turn: serverTurn, turnId, refresh, players, storyteller, loading = false, connection = "connected" } = useGameTurnState({
        gameId,
        jwt,
        setMessage,
        setVisible,
    });
    const turn=game.cancelled?{...serverTurn,state:"CANCELLED"}:serverTurn;
    const navigate = useNavigate();
    const ready = connection === "connected" && !loading;
    const busy = Boolean(action) || !ready;
    const beginAction = name => {
        if (actionRef.current) return false;
        actionRef.current = true;
        setAction(name);
        setVisible(false);
        return true;
    };
    const finishAction = () => {actionRef.current = false; setAction(null);};

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
            return Array.from({length: player?.handSize || 0}, (_, index) => ({id: `back-${index}`}));
        }
        return cards.filter((card) => card?.id == null || !playedCardIds.has(card.id));
    };

    const selectCard = async (card) => {
        if (!turnId || !currentPlayer?.id) {
            console.warn("Cannot select card without a valid turn or player");
            return;
        }
        if (!ready || game.endDate || game.deleted || !beginAction("Sending card…")) return;
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
            await refresh();
        } catch (error) {
            setMessage(error.message || "Unable to select this card.");
            setVisible(true);
            setPendingPlayedCardIds((prev) => prev.filter((id) => id !== card.id));
        } finally {finishAction();}
    };

    useEffect(() => {setClueDraft("");setClueError(null);}, [turnId]);

    function handleChange(event) {setClueDraft(event.target.value);setClueError(null);}

    async function handleSendClue(event) {
        event.preventDefault();
        if (!turnId || !isStoryteller || turn?.state !== "WAITING_STORYTELLER_CLUE" || !clueDraft.trim() || !ready || game.endDate || game.cancelled || game.deleted || !beginAction("Sending clue…")) return;
        setClueError(null);
        try {
            const response = await fetch(`/api/v1/turns/${turnId}/clue`, {
                method: "PUT",
                headers: {Authorization: `Bearer ${jwt}`, Accept: "application/json", "Content-Type": "application/json"},
                body: clueDraft,
            });
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || `Unable to set clue (${response.status}).`);
            await refresh();
        } catch (error) {setClueError(error.message || "Unable to send your clue. Please try again.");}
        finally {finishAction();}
    }

    const voteCard = async (card) => {
        if (!turnId || !currentPlayer?.id) {
            console.warn("Cannot select card without a valid turn or player");
            return;
        }
        if (!ready || game.endDate || game.deleted || !beginAction("Sending vote…")) return;
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
            await refresh();
        } catch (error) {
            setMessage(error.message || "Unable to vote for this card.");
            setVisible(true);
        } finally {finishAction();}
    };

    async function leaveGame(){
        if (!beginAction("Leaving game…")) return;
        try{
            const response=await fetch(`/api/v1/games/${gameId}/leave`,{method:"PATCH",headers:{Authorization:`Bearer ${jwt}`}});
            if(!response.ok){const error=await response.json().catch(()=>({}));throw new Error(error.message||"Unable to leave the game.");}
            navigate("/");
        }catch(error){setMessage(error.message);setVisible(true);}
        finally {finishAction();}
    }
    const opponents = <>
        <LeftPlayerPanel currentPlayer={currentPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller} getPlayerHandCards={getPlayerHandCards} />
        <TopPlayerPanel currentPlayer={currentPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller} getPlayerHandCards={getPlayerHandCards} />
        <RightPlayerPanel currentPlayer={currentPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller} getPlayerHandCards={getPlayerHandCards} />
    </>;
    return (
        <GameLayout gameId={gameId} game={game} loading={loading} connection={connection}
            storytellerName={getPlayerName(storyteller)} role={isStoryteller ? "You are the storyteller" : "You are a player"}
            message={visible ? message : null} onDismiss={() => setVisible(false)} onRetry={refresh}
            toolbar={<>
                <Button className="leave-game" disabled={Boolean(action)} onClick={leaveGame}>Leave game</Button>
                {game.leader?.id === currentUser?.id && !game.endDate && <span className="game-help">As host, leaving cancels this game.</span>}
            </>}
            opponents={opponents}
            hand={<OwnPlayerPanel currentPlayer={currentPlayer} currentUser={currentUser} isStoryteller={isStoryteller} turn={turn}
                selectCard={selectCard} getPlayerName={getPlayerName} handCards={getPlayerHandCards(currentPlayer)} busy={busy} />}
            chat={<Chat gameId={gameId} />}>
            {action && <p role="status" className="game-action-status">{action}</p>}
            <TurnStatusPanel game={game} turn={turn?.state === "WAITING_STORYTELLER_CLUE" ? {...turn, clue: clueDraft} : turn}
                currentPlayer={currentPlayer} isStoryteller={isStoryteller} storyteller={storyteller} handleChange={handleChange}
                handleSendClue={handleSendClue} voteCard={voteCard} getPlayerName={getPlayerName} busy={busy}
                cluePending={action === "Sending clue…"} clueError={clueError} />
        </GameLayout>
    );
}

import {useCallback, useEffect, useRef, useState} from "react";
import {Client} from "@stomp/stompjs";
import SockJS from "sockjs-client";

// Every notification reloads a single actor-specific snapshot; no shared event carries a hand.
export default function useGameTurnState({gameId, jwt, setMessage, setVisible, perspectivePlayerId = null}) {
    const [game, setGame] = useState({id: gameId, players: [], turns: [], winners: []});
    const [loading, setLoading] = useState(true);
    const [connection, setConnection] = useState("connecting");
    const turn = game.turns?.length ? game.turns[game.turns.length - 1] : {playedCards: []};
    const turnId = turn?.id || null;
    const requestRef = useRef(0);
    const clientRef = useRef(null);
    const turnSubscriptionRef = useRef(null);
    const turnIdRef = useRef(turnId);
    const refreshRef = useRef(() => {});
    const perspective = perspectivePlayerId == null ? "" : `?perspectivePlayerId=${encodeURIComponent(perspectivePlayerId)}`;

    const refresh = useCallback(async () => {
        if (!gameId || !jwt) return;
        const request = ++requestRef.current;
        try {
            const response = await fetch(`/api/v1/games/${gameId}${perspective}`, {headers: {Authorization: `Bearer ${jwt}`, Accept: "application/json"}});
            const payload = await response.json().catch(() => ({}));
            if (request !== requestRef.current) return;
            if (!response.ok) throw new Error(payload.message || `Unable to load game (${response.status}).`);
            setGame(payload);
            if (clientRef.current?.client.connected) setConnection("connected");
        } catch (error) {
            if (request !== requestRef.current) return;
            setMessage(error.message || "Unable to load the game.");
            setVisible(true);
        } finally {
            if (request === requestRef.current) setLoading(false);
        }
    }, [gameId, jwt, perspective, setMessage, setVisible]);
    refreshRef.current = refresh;
    turnIdRef.current = turnId;

    const setTurn = useCallback(value => setGame(previous => {
        if (!previous.turns?.length) return previous;
        const rounds = [...previous.turns];
        rounds[rounds.length - 1] = typeof value === "function" ? value(rounds[rounds.length - 1]) : value;
        return {...previous, turns: rounds};
    }), []);

    const invalidateRequests = useCallback(() => {requestRef.current++;}, []);

    useEffect(() => {
        setLoading(true);
        setGame({id: gameId, players: [], turns: [], winners: []});
        refresh();
        return invalidateRequests;
    }, [refresh, gameId, invalidateRequests]);

    useEffect(() => {
        if (!gameId || !jwt) return;
        let stopped = false;
        setConnection("connecting");
        const receive = message => {
            if (stopped || !message.body) return;
            try {
                const event = JSON.parse(message.body);
                if (event.type === "DELETED") {
                    requestRef.current++;
                    setLoading(false);
                    setGame({id: gameId, players: [], turns: [], winners: [], deleted: true});
                    return;
                }
                if (event.gameId === Number(gameId) && (event.type === "GAME_UPDATED" || event.type === "TURN_UPDATED")) refreshRef.current();
            } catch (error) {
                setMessage("Invalid game notification."); setVisible(true);
            }
        };
        const client = new Client({
            webSocketFactory: () => new SockJS("/ws"),
            connectHeaders: {Authorization: `Bearer ${jwt}`},
            reconnectDelay: 1000, heartbeatIncoming: 2000, heartbeatOutgoing: 2000, heartbeatStrategy: "worker",
        });
        const subscribeTurn = () => {
            turnSubscriptionRef.current?.unsubscribe();
            turnSubscriptionRef.current = turnIdRef.current ? client.subscribe(`/topic/turn/${turnIdRef.current}`, receive) : null;
        };
        client.onConnect = () => {
            if (stopped) return;
            setConnection("refreshing");
            client.subscribe(`/topic/game/${gameId}`, receive);
            subscribeTurn();
            refreshRef.current();
        };
        client.onStompError = () => {if (!stopped) {setConnection("error");setMessage("Unable to authenticate or subscribe to this game.");setVisible(true);}};
        client.onWebSocketClose = () => {
            turnSubscriptionRef.current = null;
            if (!stopped) setConnection("reconnecting");
        };
        clientRef.current = {client, subscribeTurn};
        client.activate();
        return () => {stopped = true; clientRef.current = null;turnSubscriptionRef.current = null;client.deactivate();};
    }, [gameId, jwt, setMessage, setVisible]);

    useEffect(() => {if (clientRef.current?.client.connected) clientRef.current.subscribeTurn();}, [turnId]);
    return {game, turn, turnId, loading, connection, setGame, setTurn, refresh, players: game.players || [], storyteller: turn?.storyteller || null};
}

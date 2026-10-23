import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import useFetchState from "../util/useFetchState";


export default function useGameTurnState({gameId, jwt, setMessage, setVisible}) {
    const emptyGame = {
        id: gameId,
        players: [],
        turns: [],
        winners: []
    };
    const [game, setGame] = useFetchState(emptyGame, `/api/v1/games/${gameId}`, jwt, setMessage, setVisible, gameId)

    const lastTurn = game.turns?.length ? game.turns[game.turns.length - 1] : null;
    const turnId = lastTurn?.id || null;

    const emptyTurn = {
        id: turnId,
        playedCards: []
    }
    const turnUrl = turnId ? `/api/v1/turns/${turnId}` : null;
    const [turn, setTurn] = useFetchState(emptyTurn, turnUrl, jwt, setMessage, setVisible, turnId)

    // Single STOMP connection reused for game + turn topics
    const clientRef = useRef(null);
    const gameSubRef = useRef(null);
    const turnSubRef = useRef(null);
    const [connected, setConnected] = useState(false);

    const resubscribeRef = useRef(() => {});

    const resubscribe = () => {
        const client = clientRef.current;
        if (!client || !client.connected) {
            return;
        }
        if (gameSubRef.current) {
            gameSubRef.current.unsubscribe();
            gameSubRef.current = null;
        }
        if (turnSubRef.current) {
            turnSubRef.current.unsubscribe();
            turnSubRef.current = null;
        }
        if (gameId) {
            gameSubRef.current = client.subscribe(`/topic/game/${gameId}`, (message) => {
                if (message.body) {
                    setGame(JSON.parse(message.body));
                }
            });
        }
        if (turnId) {
            turnSubRef.current = client.subscribe(`/topic/turn/${turnId}`, (message) => {
                if (message.body) {
                    setTurn(JSON.parse(message.body));
                }
            });
        }
    };

    resubscribeRef.current = resubscribe;

    useEffect(() => {
        // Update subscriptions when ids change or after a reconnect
        resubscribeRef.current();
    }, [gameId, turnId, connected]);

    useEffect(() => {
        if (!jwt) {
            return;
        }

        const client = new Client({
            webSocketFactory: () => new SockJS("/ws"),
            connectHeaders: {
                Authorization: `Bearer ${jwt}`,
            },
            reconnectDelay: 500,
            heartbeatIncoming: 2000,
            heartbeatOutgoing: 2000,
        });

        client.onConnect = () => {
            setConnected(true);
            resubscribeRef.current();
        };
        client.onStompError = (frame) => {
            setConnected(false);
            console.error("Broker reported error: " + frame.headers["message"]);
            console.error("Additional details: " + frame.body);
        };
        client.onDisconnect = () => {
            setConnected(false);
        };
        client.onWebSocketClose = () => {
            setConnected(false);
        };

        client.activate();
        clientRef.current = client;

        return () => {
            if (gameSubRef.current) {
                gameSubRef.current.unsubscribe();
                gameSubRef.current = null;
            }
            if (turnSubRef.current) {
                turnSubRef.current.unsubscribe();
                turnSubRef.current = null;
            }
            setConnected(false);
            if (clientRef.current) {
                clientRef.current.deactivate();
                clientRef.current = null;
                console.log("STOMP Client deactivated");
            }
        };
    }, [jwt]);

    return {
        game,
        turn,
        turnId,
        setGame,
        setTurn,
        players: game.players || [],
        storyteller: lastTurn?.storyteller || null
    };

}

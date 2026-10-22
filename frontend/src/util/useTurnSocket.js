import { useCallback, useEffect, useRef, useState } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

export default function useTurnSocket(turnId, onTurnUpdate, jwt) {
    const [connected, setConnected] = useState(false);
    const clientRef = useRef(null);
    const subscriptionRef = useRef(null);
    const turnIdRef = useRef(turnId);
    const onTurnUpdateRef = useRef(onTurnUpdate);

    useEffect(() => {
        onTurnUpdateRef.current = onTurnUpdate;
    }, [onTurnUpdate]);

    useEffect(() => {
        turnIdRef.current = turnId;
    }, [turnId]);

    const subscribeToTurn = useCallback(() => {
        const client = clientRef.current;
        const currentTurnId = turnIdRef.current;

        if (!client || !client.connected || !currentTurnId) {
            return;
        }

        if (subscriptionRef.current) {
            subscriptionRef.current.unsubscribe();
            subscriptionRef.current = null;
        }

        subscriptionRef.current = client.subscribe(`/topic/turn/${currentTurnId}`, (message) => {
            if (message.body) {
                const updatedTurn = JSON.parse(message.body);
                if (onTurnUpdateRef.current) {
                    onTurnUpdateRef.current(updatedTurn);
                }
            }
        });
    }, []);

    useEffect(() => {
        if (!jwt) {
            return;
        }

        const client = new Client({
            connectHeaders: {
                Authorization: `Bearer ${jwt}`,
            },
            debug: (str) => {
                console.log(str);
            },
            reconnectDelay: 1000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.webSocketFactory = () => new SockJS("/ws");

        client.onConnect = () => {
            console.log("Connected to STOMP");
            setConnected(true);
            subscribeToTurn();
        };

        client.onStompError = (frame) => {
            setConnected(false);
            console.error("Broker reported error: " + frame.headers["message"]);
            console.error("Additional details: " + frame.body);
        };

        client.onDisconnect = () => {
            setConnected(false);
            subscriptionRef.current = null;
        };

        client.activate();
        clientRef.current = client;

        return () => {
            if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
                subscriptionRef.current = null;
            }
            if (clientRef.current) {
                clientRef.current.deactivate();
                clientRef.current = null;
                console.log("STOMP Client deactivated");
            }
            setConnected(false);
        };
    }, [jwt, subscribeToTurn]);

    // Re-subscribe quickly when turnId changes without rebuilding the connection
    useEffect(() => {
        subscribeToTurn();
    }, [turnId, subscribeToTurn]);

    return connected;
}

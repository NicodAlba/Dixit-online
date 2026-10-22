import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useEffect, useRef } from "react";

export default function useGameSocket(gameId, onGameUpdate, jwt) {
    const onGameUpdateRef = useRef(onGameUpdate);

    useEffect(() => {
        onGameUpdateRef.current = onGameUpdate;
    }, [onGameUpdate]);

    useEffect(() => {
        if (!gameId) return;

        const client = new Client({
            // Modern way to use SockJS with @stomp/stompjs
            webSocketFactory: () => new SockJS("/ws"),
            connectHeaders: {
                Authorization: `Bearer ${jwt}`,
            },
            // Important for browser compatibility
            debug: (str) => { console.log(str); },
            reconnectDelay: 1000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = (frame) => {
            console.log('Connected: ' + frame);
            client.subscribe(`/topic/game/${gameId}`, (message) => {
                if (message.body) {
                    const game = JSON.parse(message.body);
                    if (onGameUpdateRef.current) {
                        onGameUpdateRef.current(game);
                    }
                }
            });
        };

        client.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        client.activate();

        return () => {
            if (client.active) {
                client.deactivate();
            }
        };
    }, [gameId, jwt]);
}

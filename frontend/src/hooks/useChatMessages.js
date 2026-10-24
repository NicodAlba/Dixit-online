import {useCallback, useEffect, useRef, useState} from "react";
import tokenService from "../services/token.service";

async function requireSuccess(response, fallback) {
    if (response.ok) return;
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `${fallback} (${response.status}).`);
}

// Each game owns its requests. A late response must never replace another game's chat.
export default function useChatMessages(gameId) {
    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [loadError, setLoadError] = useState(null);
    const [sendError, setSendError] = useState(null);
    const sessionRef = useRef(null);
    const jwt = tokenService.getLocalAccessToken();

    useEffect(() => {
        const session = {active: true, request: 0, sending: false, controllers: new Set()};
        sessionRef.current = session;
        setMessages([]);
        setLoading(Boolean(gameId && jwt));
        setSending(false);
        setLoadError(null);
        setSendError(null);
        let timer;
        const url = `/api/v1/messages/games/${gameId}`;
        const headers = {Authorization: `Bearer ${jwt}`, "Content-Type": "application/json"};

        session.refresh = async () => {
            const request = ++session.request;
            const controller = new AbortController();
            session.controllers.add(controller);
            try {
                const response = await fetch(url, {headers, signal: controller.signal});
                await requireSuccess(response, "Unable to load chat");
                const data = await response.json();
                if (!session.active || request !== session.request) return;
                setMessages(data);
                setLoadError(null);
            } catch (error) {
                if (session.active && request === session.request && error.name !== "AbortError") {
                    setLoadError(error.message || "Unable to load chat.");
                }
            } finally {
                session.controllers.delete(controller);
                if (session.active && request === session.request) setLoading(false);
            }
        };
        session.send = async text => {
            if (session.sending) return false;
            session.sending = true;
            setSending(true);
            setSendError(null);
            const controller = new AbortController();
            session.controllers.add(controller);
            try {
                const response = await fetch(url, {
                    method: "POST", headers, signal: controller.signal,
                    body: JSON.stringify({text}),
                });
                await requireSuccess(response, "Could not send message");
                if (!session.active) return false;
                // Invalidate older polls before refreshing; do not repeat the POST on reconnect.
                await session.refresh();
                return session.active;
            } catch (error) {
                if (session.active && error.name !== "AbortError") {
                    setSendError(error.message || "Could not send message.");
                }
                return false;
            } finally {
                session.controllers.delete(controller);
                session.sending = false;
                if (session.active) setSending(false);
            }
        };
        async function poll() {
            await session.refresh();
            if (session.active) timer = setTimeout(poll, 3000);
        }
        if (gameId && jwt) poll();
        return () => {
            session.active = false;
            clearTimeout(timer);
            session.controllers.forEach(controller => controller.abort());
        };
    }, [gameId, jwt]);

    const send = useCallback(async text => {
        if (!text.trim()) return false;
        if (!tokenService.getLocalAccessToken()) {
            setSendError("Your session has ended. Please log in again.");
            return false;
        }
        return sessionRef.current?.active ? sessionRef.current.send(text) : false;
    }, []);

    return {messages, loading, sending, error: sendError || loadError, send};
}

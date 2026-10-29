import {useCallback, useEffect, useRef, useState} from "react";
import tokenService from "../services/token.service";
import {useApplicationConnection} from "../presence/PresenceContext";

async function requireSuccess(response, fallback) {
    if (response.ok) return;
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `${fallback} (${response.status}).`);
}

// Each game owns its requests. A late response must never replace another game's chat.
export default function useChatMessages(gameId) {
    const [messageSnapshot, setMessages] = useState(null);
    const [messageUpdates, setNewMessages] = useState(null);
    const [arrivingIds, setArrivingIds] = useState([]);
    const connection = useApplicationConnection();
    const connectionState = connection?.state;
    const generation = connection?.generation;
    const accountId = tokenService.getUser()?.id;
    const available = !connectionState || connectionState === "connected";
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [loadError, setLoadError] = useState(null);
    const [sendError, setSendError] = useState(null);
    const sessionRef = useRef(null);
    const jwt = tokenService.getLocalAccessToken();

    useEffect(() => {
        const session = {active: true, request: 0, sending: false, controllers: new Set(), seen: new Set(), baseline: false};
        sessionRef.current = session;
        setMessages(null);
        setNewMessages(null);
        setArrivingIds([]);
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
                if (!Array.isArray(data)) throw new Error("Unable to load chat.");
                const arrivals = data.filter(message => message.id != null && !session.seen.has(message.id));
                const incoming = arrivals.filter(message => message.accountId !== accountId);
                data.forEach(message => {if (message.id != null) session.seen.add(message.id);});
                if (session.baseline && incoming.length) setNewMessages({gameId, jwt, items: incoming});
                if (session.baseline && arrivals.length) setArrivingIds(arrivals.map(message => message.id));
                session.baseline = true;
                setMessages({gameId, jwt, items: data});
                setLoadError(null);
            } catch (error) {
                if (session.active && request === session.request && error.name !== "AbortError") {
                    session.baseline = false;
                    setLoadError(error.message || "Unable to load chat.");
                }
            } finally {
                session.controllers.delete(controller);
                if (session.active && request === session.request) setLoading(false);
            }
        };
        session.send = async text => {
            if (session.sending || !available) return false;
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
        if (gameId && jwt && available) poll();
        return () => {
            session.active = false;
            clearTimeout(timer);
            session.controllers.forEach(controller => controller.abort());
        };
    }, [gameId, jwt, accountId, available, generation]);

    const send = useCallback(async text => {
        if (!text.trim()) return false;
        if (!tokenService.getLocalAccessToken()) {
            setSendError("Your session has ended. Please log in again.");
            return false;
        }
        return sessionRef.current?.active ? sessionRef.current.send(text) : false;
    }, []);

    return {messages: messageSnapshot?.gameId === gameId && messageSnapshot?.jwt === jwt ? messageSnapshot.items : [],
        newMessages: messageUpdates?.gameId === gameId && messageUpdates?.jwt === jwt ? messageUpdates.items : null, arrivingIds, available, loading, sending, error: sendError || loadError, send};
}

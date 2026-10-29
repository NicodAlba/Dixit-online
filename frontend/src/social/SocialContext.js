import React, {createContext, useCallback, useContext, useEffect, useRef, useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import tokenService from "../services/token.service";
import SocialPanel from "./SocialPanel";
import {useApplicationConnection} from "../presence/PresenceContext";
import SocialNotifications from "./SocialNotifications";
import {socialChanges} from "./socialChanges";

const SocialContext = createContext(null);
export const useSocial = () => useContext(SocialContext);

export function SocialProvider({jwt, children}) {
    const [open, setOpen] = useState(false);
    const [section, setSection] = useState("friends");
    const [notifications, setNotifications] = useState([]);
    const baseline = useRef(null);
    const seen = useRef(new Set());
    const connection = useApplicationConnection();
    const connectionState = connection?.state;
    const generation = connection?.generation;
    const [friends, setFriends] = useState([]);
    const [relationships, setRelationships] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loaded, setLoaded] = useState(false);
    const [error, setError] = useState(null);
    const [feedback, setFeedback] = useState(null);
    const [pending, setPending] = useState(false);
    const pendingRef = useRef(false);
    const requestRevision = useRef(0);
    const alive = useRef(true);
    const location = useLocation();
    const navigate = useNavigate();
    const invalidateRequests = useCallback(() => {requestRevision.current++;}, []);
    useEffect(() => {alive.current = true; return () => {alive.current = false; invalidateRequests();};}, [invalidateRequests]);
    useEffect(() => {if (location.pathname !== "/friends") setOpen(false);}, [location.pathname]);

    const request = useCallback(async (path, method = "GET", body) => {
        const response = await fetch(path, {method, headers: {Authorization: `Bearer ${jwt}`, "Content-Type": "application/json"}, ...(body === undefined ? {} : {body: JSON.stringify(body)})});
        const data = await response.json().catch(() => null);
        if (!response.ok) throw new Error(data?.message || (response.status === 401 ? "Your session expired. Please log in again." : "Unable to complete this request. Please try again."));
        return data;
    }, [jwt]);
    const refresh = useCallback(async () => {
        const revision = ++requestRevision.current;
        setLoading(true);
        try {
            const [[friends, relationships], invitationResult] = await Promise.all([
                Promise.all([request("/api/v1/friends"), request("/api/v1/friends/relationships")]),
                request("/api/v1/games/invitations").then(data => ({data}), () => ({data: null})),
            ]);
            if (!alive.current || revision !== requestRevision.current) return;
            if (![friends, relationships].every(Array.isArray)) throw new Error("Unable to load social updates. Please try again.");
            // Invitation failure must not disable friendship actions. Its section retains its own error feedback.
            if (Array.isArray(invitationResult.data)) {
                const snapshot = {relationships, invitations: invitationResult.data};
                const changes = socialChanges(baseline.current, snapshot, seen.current);
                baseline.current = snapshot;
                if (changes.length) setNotifications(previous => [...previous, ...changes].slice(-3));
            } else baseline.current = null;
            setFriends(friends); setRelationships(relationships); setError(null); setLoaded(true);
        } catch (error) {if (alive.current && revision === requestRevision.current) {baseline.current = null; setError(error.message);}}
        finally {if (alive.current && revision === requestRevision.current) setLoading(false);}
    }, [request]);
    useEffect(() => {
        baseline.current = null;
        if (connectionState && connectionState !== "connected") return;
        refresh();
        const timer = setInterval(refresh, 10000);
        return () => {clearInterval(timer); invalidateRequests();};
    }, [connectionState, generation, refresh, invalidateRequests]);

    const perform = useCallback(async (action, success) => {
        if (pendingRef.current) return false;
        pendingRef.current = true; setPending(true); setError(null); setFeedback(null);
        try {
            await action();
            if (!alive.current) return false;
            setFeedback(success); await refresh(); return true;
        } catch (error) {if (alive.current) setError(error.message || "Unable to complete this action."); return false;}
        finally {pendingRef.current = false; if (alive.current) setPending(false);}
    }, [refresh]);
    const openPanel = useCallback((target) => {if (typeof target === "string") setSection(target); setOpen(true);}, []);
    const notifyChat = useCallback((gameId, messages, onView) => {
        const updates = messages.map(message => ({id: `chat:${gameId}:${message.id}`, section: "chat",
            message: `${message.username}: ${message.text}`, onView}));
        setNotifications(previous => [...previous, ...updates.filter(item => !previous.some(old => old.id === item.id))].slice(-3));
    }, []);
    const clearChatNotifications = useCallback(gameId => setNotifications(previous => previous.filter(item => !item.id.startsWith(`chat:${gameId}:`))), []);
    const dismissNotification = useCallback(id => setNotifications(previous => previous.filter(item => item.id !== id)), []);
    const closePanel = () => {
        setOpen(false);
        if (location.pathname === "/friends") navigate("/", {replace: true});
    };
    const relation = accountId => relationships.find(item => item.accountId === accountId);
    const context = {
        open, openPanel, closePanel, section, setSection, notifications, dismissNotification, friends, relationships, loading, loaded, error, feedback, pending,
        refresh, request, perform, relation, notifyChat, clearChatNotifications, selfId: tokenService.getUser()?.id,
        sendRequest: accountId => perform(() => request(`/api/v1/friends/addFriend/${accountId}`, "POST"), "Friend request sent."),
        acceptRequest: requestId => perform(() => request(`/api/v1/friends/${requestId}/accept`, "PATCH"), "Friend request accepted."),
        denyRequest: requestId => perform(() => request(`/api/v1/friends/${requestId}/deny`, "DELETE"), "Friend request declined."),
        removeFriend: accountId => perform(() => request(`/api/v1/friends/${accountId}/remove`, "DELETE"), "Friend removed."),
    };
    return <SocialContext.Provider value={context}>{children}<SocialNotifications /><SocialPanel /></SocialContext.Provider>;
}

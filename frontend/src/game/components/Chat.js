import React, {useCallback, useEffect, useRef, useState} from "react";
import tokenService from "../../services/token.service";
import {useSocial} from "../../social/SocialContext";
import AccountMenu from "../../social/AccountMenu";
import useChatMessages from "../../hooks/useChatMessages";
import "../../static/css/chat/chat.css";

export default function Chat({gameId}) {
    const {messages, newMessages, arrivingIds = [], available = true, loading, sending, error, send} = useChatMessages(gameId);
    const social = useSocial();
    const notifyChat = social?.notifyChat;
    const clearChatNotifications = social?.clearChatNotifications;
    const inputRef = useRef(null);
    const [focusRequest, setFocusRequest] = useState(0);
    const [newMessage, setNewMessage] = useState("");
    const messagesRef = useRef(null);
    const user = tokenService.getUser();
    const [open, setOpen] = useState(() => window.matchMedia ? window.matchMedia("(min-width: 901px)").matches : true);
    const followMessagesRef = useRef(true);
    const openChat = useCallback(() => {setOpen(true); setFocusRequest(value => value + 1);}, []);
    useEffect(() => {if (focusRequest) inputRef.current?.focus();}, [focusRequest]);
    useEffect(() => {if (newMessages?.length) notifyChat?.(gameId, newMessages, openChat);}, [gameId, newMessages, notifyChat, openChat]);
    useEffect(() => () => clearChatNotifications?.(gameId), [gameId, clearChatNotifications]);
    useEffect(() => {
        if (!window.matchMedia) return;
        const query = window.matchMedia("(min-width: 901px)");
        const update = event => setOpen(event.matches);
        query.addEventListener("change", update);
        return () => query.removeEventListener("change", update);
    }, []);

    useEffect(() => setNewMessage(""), [gameId]);
    useEffect(() => {
        const panel = messagesRef.current;
        if (panel && followMessagesRef.current) panel.scrollTop = panel.scrollHeight;
    }, [messages, open]);

    async function sendMessage(event) {
        event.preventDefault();
        if (await send(newMessage)) setNewMessage("");
    }

    return (
        <details className="chat-container" open={open} onToggle={event => setOpen(event.currentTarget.open)}>
            <summary>Chat <span>· game participants</span></summary>
            <div className="chat-content">
            <div className="chat-messages" ref={messagesRef} role="log" aria-live="off" aria-label="Messages"
                onScroll={event => {
                    const panel = event.currentTarget;
                    followMessagesRef.current = panel.scrollHeight - panel.scrollTop - panel.clientHeight < 48;
                }}>
                {!available ? <p role="status">Reconnecting chat…</p> : loading && <p role="status">Loading chat…</p>}
                {messages.map((msg, index) => (
                    <div key={msg.id ?? index} className={`chat-message ${arrivingIds.includes(msg.id) ? "message-arriving" : ""} ${msg.accountId != null && msg.accountId === user?.id ? "my-message" : "other-message"}`}>
                        <AccountMenu className="message-sender" accountId={msg.accountId} username={msg.username} />
                        <span className="message-text">{msg.text}</span>
                    </div>
                ))}
            </div>
            {error && <div role="alert" className="chat-error">{error}</div>}
            <form className="chat-input-form" onSubmit={sendMessage}>
                <input
                    type="text"
                    ref={inputRef}
                    aria-label="Chat message"
                    value={newMessage}
                    disabled={sending || !available}
                    onChange={event => setNewMessage(event.target.value)}
                    placeholder="Type a message..."
                    maxLength="255"
                />
                <button type="submit" disabled={sending || !available || !newMessage.trim()}>{sending ? "Sending…" : "Send"}</button>
            </form>
            </div>
        </details>
    );
}

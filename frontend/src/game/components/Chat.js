import React, {useEffect, useRef, useState} from "react";
import tokenService from "../../services/token.service";
import useChatMessages from "../../hooks/useChatMessages";
import "../../static/css/chat/chat.css";

export default function Chat({gameId}) {
    const {messages, loading, sending, error, send} = useChatMessages(gameId);
    const [newMessage, setNewMessage] = useState("");
    const messagesRef = useRef(null);
    const user = tokenService.getUser();
    const [open, setOpen] = useState(() => window.matchMedia ? window.matchMedia("(min-width: 901px)").matches : true);
    const followMessagesRef = useRef(true);
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
            <div className="chat-messages" ref={messagesRef} role="log" aria-live="polite" aria-label="Messages"
                onScroll={event => {
                    const panel = event.currentTarget;
                    followMessagesRef.current = panel.scrollHeight - panel.scrollTop - panel.clientHeight < 48;
                }}>
                {loading && <p role="status">Loading chat…</p>}
                {messages.map((msg, index) => (
                    <div key={index} className={`chat-message ${msg.username === user?.username ? "my-message" : "other-message"}`}>
                        <span className="message-sender">{msg.username}</span>
                        <span className="message-text">{msg.text}</span>
                    </div>
                ))}
            </div>
            {error && <div role="alert" className="chat-error">{error}</div>}
            <form className="chat-input-form" onSubmit={sendMessage}>
                <input
                    type="text"
                    aria-label="Chat message"
                    value={newMessage}
                    disabled={sending}
                    onChange={event => setNewMessage(event.target.value)}
                    placeholder="Type a message..."
                    maxLength="255"
                />
                <button type="submit" disabled={sending || !newMessage.trim()}>{sending ? "Sending…" : "Send"}</button>
            </form>
            </div>
        </details>
    );
}

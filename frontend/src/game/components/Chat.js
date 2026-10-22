import React, { useEffect, useState, useRef } from 'react';
import tokenService from '../../services/token.service';
import getErrorModal from '../../util/getErrorModal';
import "../../static/css/chat/chat.css";

export default function Chat({ gameId }) {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState("");
    const [user, setUser] = useState(null);
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }

    useEffect(() => {
        const currentUser = tokenService.getUser();
        setUser(currentUser);
        getMessages();

        const interval = setInterval(() => {
            getMessages();
        }, 3000);

        return () => clearInterval(interval);
    }, [gameId]);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    async function getMessages() {
        const jwt = tokenService.getLocalAccessToken();
        if (!jwt) return;

        try {
            const response = await fetch(`/api/v1/messages/games/${gameId}`, {
                headers: {
                    "Authorization": `Bearer ${jwt}`,
                    "Content-Type": "application/json"
                }
            });
            if (response.ok) {
                const data = await response.json();
                setMessages(data);
            }
        } catch (error) {
            console.error("Error fetching messages:", error);
        }
    }

    async function sendMessage(e) {
        e.preventDefault();
        if (!newMessage.trim()) return;

        const jwt = tokenService.getLocalAccessToken();
        if (!jwt) return;

        try {
            const response = await fetch(`/api/v1/messages/games/${gameId}`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${jwt}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ text: newMessage })
            });

            if (response.ok) {
                setNewMessage("");
                getMessages(); // Refresh immediately
            } else {
                const errorData = await response.json();
                getErrorModal(errorData.message);
            }
        } catch (error) {
            getErrorModal("Could not send message");
        }
    }

    return (
        <div className="chat-container">
            <div className="chat-messages">
                {messages.map((msg, index) => {
                    const isMe = msg.username === user.username;
                    return (
                        <div key={index} className={`chat-message ${isMe ? 'my-message' : 'other-message'}`}>
                            <span className="message-sender">{msg.username}</span>
                            <span className="message-text">{msg.text}</span>
                        </div>
                    );
                })}
                <div ref={messagesEndRef} />
            </div>
            <form className="chat-input-form" onSubmit={sendMessage}>
                <input
                    type="text"
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    placeholder="Type a message..."
                    maxLength="255"
                />
                <button type="submit">Send</button>
            </form>
        </div>
    );
}

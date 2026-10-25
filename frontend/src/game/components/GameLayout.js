import React from "react";
import {ParticipantPresence} from "../../presence/ParticipantPresence";
import SocialToggle from "../../social/SocialToggle";

export default function GameLayout({gameId, game, loading, connection = "connected", storytellerName, role, message, onDismiss, onRetry, toolbar, opponents, hand, chat, children}) {
    const rounds = game.turns?.length || 0;
    const total = (game.players?.length || 0) * 2;
    const connectionText = {
        connecting: "Connecting to the game…",
        refreshing: "Connected. Refreshing your game…",
        reconnecting: "Reconnecting… Your place is preserved. Actions will return after the game refreshes.",
        error: "Unable to connect. Check your session and connection.",
    }[connection];
    return (
        <main className="game-page-container">
            <ParticipantPresence gameId={gameId} players={game.players}>
            <header className="game-header">
                <div>
                    <div className="game-overline">DIXIT · Game {gameId}</div>
                    <h1>{game.cancelled ? "Game cancelled" : game.endDate ? "Game finished" : rounds ? `Round ${rounds} of ${total}` : "Preparing the game"}</h1>
                    <p>{storytellerName && `Storyteller: ${storytellerName} · `}{role}</p>
                </div>
                <div className="game-toolbar"><SocialToggle />{toolbar}</div>
            </header>
            {connectionText && <div className="game-notice" role="status">{connectionText}</div>}
            {loading && <div className="game-notice" role="status">Loading your game…</div>}
            {message && <div className="game-notice game-error" role="alert">
                <span>{message}</span>
                <button type="button" onClick={onRetry}>Refresh game</button>
                <button type="button" onClick={onDismiss} aria-label="Dismiss message">×</button>
            </div>}
            {game.cancelled && <div className="game-notice" role="alert">Game cancelled. Scores and history are preserved; no new winners or rewards are awarded.</div>}
            {game.deleted && <div className="game-notice" role="alert">This game has been deleted.</div>}
            <div className={`game-layout ${chat ? "with-chat" : ""}`}>
                <div className="game-board">
                    <section className="game-opponents" aria-label="Other players">{opponents}</section>
                    <section className="game-turn-panel" aria-label="Game table">{children}</section>
                    {hand}
                </div>
                {chat}
            </div>
            </ParticipantPresence>
        </main>
    );
}

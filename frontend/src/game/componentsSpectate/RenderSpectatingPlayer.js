import React from "react";
import CardPreview from "../components/CardPreview";

export function RenderSpectatingPlayer({currentPlayer, spectatingUser, isStoryteller, getPlayerName, arrivingCardIds = []}) {
    const cards = currentPlayer?.cards || [];
    return (
        <section className="owned-cards-container" aria-label="Observed hand">
            <div className="hand-heading">
                <div><strong>{getPlayerName(currentPlayer) || spectatingUser?.username || ""}</strong> · {currentPlayer?.points} pts</div>
                <span>{cards.length} cards · {isStoryteller ? "Storyteller’s perspective" : "Spectator view"}</span>
            </div>
            <ul className="game-card-list hand-cards">
                {cards.map((card,index) => <li key={card.id} className={arrivingCardIds.includes(card.id) ? "card-arriving" : ""}><div className="card-button">
                    <img className="card" src={`/Cards/${card.image?.id}.png`} alt="Card" />
                </div><CardPreview card={card} label={`observed hand card ${index + 1}`} /></li>)}
            </ul>
            <p className="game-help">You are watching this player’s authorized perspective. Actions are available to participants only.</p>
        </section>
    );
}

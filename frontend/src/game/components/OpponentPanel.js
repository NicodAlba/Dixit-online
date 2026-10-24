import React from "react";

export default function OpponentPanel({position, player, storyteller, getPlayerName, cards}) {
    const handSize = cards?.length ?? player.handSize ?? 0;
    return (
        <div className={`${position}-player-container player-panel`}>
            <div className="opponent-info">
                <strong className="opponent-name">{getPlayerName(player)}</strong>
                <span className="opponent-points">{player.points} pts · {handSize} cards</span>
                <span className="opponent-role">{player.id === storyteller?.id ? "Storyteller" : "Player"}</span>
            </div>
            <div className="opponent-backs" aria-label={`${handSize} hidden cards`}>
                {Array.from({length: handSize}, (_, index) => <img key={index} className="card" src="/Cards/card_reversed.png" alt="Card" />)}
            </div>
        </div>
    );
}

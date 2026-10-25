import React from "react";
import {ConnectionBadge} from "../../presence/ParticipantPresence";

export default function OpponentPanel({position, player, storyteller, getPlayerName, cards}) {
    const handSize = cards?.length ?? player.handSize ?? 0;
    return (
        <div className={`${position}-player-container player-panel`}>
            <div className="opponent-info">
                <div className="opponent-name-row"><strong className="opponent-name">{getPlayerName(player)}</strong><ConnectionBadge accountId={player.user?.id} /></div>
                <span className="opponent-points">{player.points} pts · {handSize} cards</span>
                <span className="opponent-role">{player.id === storyteller?.id ? "Storyteller" : "Player"}</span>
            </div>
            <div className="opponent-backs" aria-label={`${handSize} hidden cards`}>
                {Array.from({length: handSize}, (_, index) => <img key={index} className="card" src="/Cards/card_reversed.png" alt="Card" />)}
            </div>
        </div>
    );
}

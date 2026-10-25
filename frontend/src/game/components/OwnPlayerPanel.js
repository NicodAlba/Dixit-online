import React from "react";
import {ConnectionBadge} from "../../presence/ParticipantPresence";
import { List, ListInlineItem } from "reactstrap";
import fairy from "../../static/images/icons/Fairy.png";
import "../../static/css/game/Game.css";

export function OwnPlayerPanel({ currentPlayer, currentUser, isStoryteller, turn, selectCard, getPlayerName, handCards, busy = false }) {
    const playedCardIds = new Set((turn?.playedCards || []).map((card) => card.id));
    const hasPlayedCard = Boolean(turn?.hasPlayed);

    const visibleHand = handCards || (currentPlayer.cards || []).filter((card) => !playedCardIds.has(card.id));

    const canPlayCard = isStoryteller
        ? turn?.state === "WAITING_STORYTELLER_CARD"
        : turn?.state === "WAITING_PLAYER_CARDS" && !hasPlayedCard;

    const handLocked = !canPlayCard;

    const cardsList = visibleHand.map((card, index) => {
        const image = `/Cards/${card.image?.id}.png`;
        const clickable = canPlayCard && !busy;
        const cardClasses = ["card-button", clickable ? "card-clickable" : "card-disabled"].join(" ");

        const handleClick = () => {
            if (!clickable) return;
            selectCard(card);
        };

        return (
            <ListInlineItem key={card.id}>
                <button type="button" className={cardClasses} disabled={!clickable} onClick={handleClick} aria-label={`Select card ${index + 1}`}>
                    <img className="card" src={image} alt="Card" />
                </button>
            </ListInlineItem>
        );
    });

    return (
        <div className={`owned-cards-container ${handLocked ? "hand-locked" : ""}`}>
            <div className="hand-heading">
                <div><strong>{getPlayerName(currentPlayer) || currentUser?.username || "You"}</strong> · {currentPlayer.points} pts <ConnectionBadge accountId={currentPlayer.user?.id || currentUser?.id} /></div>
                <span>{visibleHand.length} cards · {isStoryteller ? "Storyteller" : "Your private hand"}</span>
            </div>
            {isStoryteller && <img className="storyteller-icon" src={fairy} alt="Storyteller" />}
            <List className="game-card-list hand-cards">{cardsList}</List>
            <p className="game-help">{turn?.state === "CANCELLED" ? "This game is cancelled. Your hand and score are preserved." : turn?.state === "FINISHED" ? "Game finished. No further cards are dealt." : canPlayCard ? "Choose the illustration that best matches the clue." : "Your hand will refill to six before the next round."}</p>
        </div>
    );
}

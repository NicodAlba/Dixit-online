import React from "react";
import { List, ListInlineItem } from "reactstrap";
import fairy from "../../static/images/icons/Fairy.png";
import "../../static/css/game/Game.css";

export function OwnPlayerPanel({ currentPlayer, currentUser, isStoryteller, turn, selectCard, getPlayerName, handCards }) {
    const playedCardIds = new Set((turn?.playedCards || []).map((card) => card.id));
    const hasPlayedCard = Boolean(turn?.hasPlayed);

    const visibleHand = handCards || (currentPlayer.cards || []).filter((card) => !playedCardIds.has(card.id));

    const canPlayCard = isStoryteller
        ? turn?.state === "WAITING_STORYTELLER_CARD"
        : turn?.state === "WAITING_PLAYER_CARDS" && !hasPlayedCard;

    const handLocked = !canPlayCard;

    const cardsList = visibleHand.map((card) => {
        const image = `/Cards/${card.image?.id}.png`;
        const clickable = canPlayCard;
        const cardClasses = ["card-button", clickable ? "card-clickable" : "card-disabled"].join(" ");

        const handleClick = () => {
            if (!clickable) return;
            selectCard(card);
        };

        return (
            <ListInlineItem key={card.id}>
                <div className={cardClasses} onClick={handleClick}>
                    <img className="card" src={image} alt="Card" />
                </div>
            </ListInlineItem>
        );
    });

    return (
        <div className={`owned-cards-container ${handLocked ? "hand-locked" : ""}`}>
            <List>
                <ListInlineItem><h3>{getPlayerName(currentPlayer) || currentUser?.username || ""}</h3></ListInlineItem>
                {isStoryteller && (
                    <ListInlineItem>
                        <img src = {fairy} alt="st" style={{width:"30px"}}/>
                    </ListInlineItem>)}
                    <h4>{currentPlayer.points} pts</h4>
                </List>
                <List>{cardsList}</List>
            </div>
        )
    }

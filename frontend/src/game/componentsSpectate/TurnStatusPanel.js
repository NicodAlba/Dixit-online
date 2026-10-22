import React from "react";
import {Button, Form, Input, Label, List, ListInlineItem} from "reactstrap";
import "../../static/css/game/Game.css"

export default function TurnStatusPanel({game, turn, currentPlayer, getPlayerName}) {
    const renderWaitingForCards = () => {
        if (turn?.state !== "WAITING_PLAYER_CARDS") {
            return null;
        }

        return (
            <div>
                <h1 className="clue">Clue: {turn?.clue}</h1>
                <h1>
                    {"Waiting for other players to choose their card"}
                </h1>
            </div>
        );
    };

    const renderPlayedCards = () => {
        if (turn?.state !== "SHOWING_CARDS") {
            return null;
        }

        const cards = turn?.playedCards || [];
        const cardsList = cards.map((card) => {
            const image = `/Cards/${card.image.id}.png`;
            const isOwnCard = card.owner?.id === currentPlayer?.id;

            return (
                <ListInlineItem key={card.id}>
                    <h3>{card.votes?.length} votes</h3>
                    <div className={"card-button"}>
                        <img className="card" src={image} alt="Card" />
                    </div>
                </ListInlineItem>
            );
        });

        return (
            <div>
                <h1 className="clue">Clue: {turn?.clue}</h1>
                <h1>{"Wait until everyone has voted"}</h1>
                <List>{cardsList}</List>
            </div>
        );
    };

    const renderStorytellerForm = () => (
        <div>
            {turn?.clue == null &&
                (turn?.state === "WAITING_STORYTELLER_CARD" || turn?.state === "WAITING_STORYTELLER_CLUE") && (
                    <h1>Waiting for storyteller</h1>
                )}
            {turn?.state === "SHOWING_CARDS" && <h1>Waiting for votes</h1>}
            {game.endDate != null && <div>
                <h1>GAME FINISHED</h1>
                <h3>Winners: {game?.winners?.map((winner) => {
                    return getPlayerName(winner)
                }).join(", ")}</h3>
                </div>}
        </div>
    );

    if (turn?.state === "WAITING_PLAYER_CARDS") {
        return renderWaitingForCards();
    }

    if (turn?.state === "SHOWING_CARDS") {
        return renderPlayedCards();
    }

    return renderStorytellerForm();
}

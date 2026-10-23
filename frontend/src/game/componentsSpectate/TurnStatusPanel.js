import React from "react";
import {List, ListInlineItem} from "reactstrap";
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
        if (!["SHOWING_CARDS", "VOTING", "FINISHED"].includes(turn?.state)) {
            return null;
        }

        const cards = turn?.playedCards || [];
        const cardsList = cards.map((card) => {
            const image = `/Cards/${card.image.id}.png`;

            return (
                <ListInlineItem key={card.id}>
                    {turn?.state === "FINISHED" && <h3>{card.votes?.length || 0} votes</h3>}
                    <div className={"card-button"}>
                        <img className="card" src={image} alt="Card" />
                    </div>
                </ListInlineItem>
            );
        });

        return (
            <div>
                <h1 className="clue">Clue: {turn?.clue}</h1>
                <h1>{turn?.state === "FINISHED" ? "Round result" : "Wait until everyone has voted"}</h1>
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
            {game.endDate != null && !game.cancelled && <div>
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

    if (turn?.state === "FINISHED") return <>{renderStorytellerForm()}{renderPlayedCards()}</>;
    if (["SHOWING_CARDS", "VOTING"].includes(turn?.state)) return renderPlayedCards();

    return renderStorytellerForm();
}

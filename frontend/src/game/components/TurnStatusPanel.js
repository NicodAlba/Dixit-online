import React from "react";
import {Button, Form, Input, Label, List, ListInlineItem} from "reactstrap";
import "../../static/css/game/Game.css"

export default function TurnStatusPanel({game, turn, currentPlayer, isStoryteller, storyteller, handleChange, handleSendClue, voteCard, getPlayerName}) {
    const renderWaitingForCards = () => {
        if (turn?.state !== "WAITING_PLAYER_CARDS") {
            return null;
        }

        return (
            <div>
                <h1 className="clue">Clue: {turn?.clue}</h1>
                <h1>
                    {isStoryteller
                        ? "Waiting for other players to choose their card"
                        : "Select your card"}
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
            const canVote = !isStoryteller && !isOwnCard && turn?.state === "SHOWING_CARDS";
            const cardClasses = [
                "card-button",
                canVote ? "card-clickable" : "card-disabled",
                isOwnCard ? "card-own" : ""
            ].join(" ");

            const handleVote = () => {
                if (!canVote) return;
                voteCard(card);
            };

            return (
                <ListInlineItem key={card.id}>
                    <h3>{card.votes?.length} votes</h3>
                    <div className={cardClasses} onClick={handleVote}>
                        <img className="card" src={image} alt="Card" />
                    </div>
                </ListInlineItem>
            );
        });

        return (
            <div>
                <h1 className="clue">Clue: {turn?.clue}</h1>
                <h1>{isStoryteller ? "Wait until everyone has voted" : "Vote a card"}</h1>
                <List>{cardsList}</List>
            </div>
        );
    };

    const renderStorytellerForm = () => (
        <div>
            {storyteller?.id === currentPlayer.id && turn?.state === "WAITING_STORYTELLER_CARD" && (
                <h1>Choose a card</h1>
            )}
            {storyteller?.id === currentPlayer.id && turn?.state === "WAITING_STORYTELLER_CLUE" && (
                <Form onSubmit={handleSendClue}>
                    <div className="custom-form-input">
                        <Label for="clue" className="clue">
                            Clue
                        </Label>
                        <Input
                            type="text"
                            required
                            name="clue"
                            id="clue"
                            value={turn?.clue || ""}
                            onChange={handleChange}
                            className="custom-input"
                        />
                    </div>
                    <div className="custom-button-row">
                        <Button color="success" type="submit">
                            Set clue
                        </Button>
                    </div>
                </Form>
            )}
            {turn?.clue == null && storyteller?.id !== currentPlayer.id &&
                (turn?.state === "WAITING_STORYTELLER_CARD" || turn?.state === "WAITING_STORYTELLER_CLUE") && (
                    <h1>Waiting for storyteller</h1>
                )}
            {!isStoryteller && turn?.state === "SHOWING_CARDS" && <h1>Choose a card to vote</h1>}
            {game.endDate != null && <div className="text-center">
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
    console.log(game)

    return renderStorytellerForm();
}

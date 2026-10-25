import React from "react";
import CardPreview from "./CardPreview";
import GameResult from "./GameResult";
import {Button, Form, Input, Label} from "reactstrap";

export default function TurnStatusPanel({game, turn, currentPlayer, isStoryteller, storyteller, handleChange, handleSendClue, voteCard, busy = false, readOnly = false}) {
    const state = turn?.state;
    const closed = state === "FINISHED";
    if (game.cancelled || game.deleted) return <><GameResult game={game} currentPlayer={currentPlayer} readOnly={readOnly} /><p>Actions are unavailable. The game history has been preserved.</p></>;
    if (!state) return <p>Waiting for the game state…</p>;
    const isNarrator = isStoryteller ?? storyteller?.id === currentPlayer?.id;
    const tableVisible = ["SHOWING_CARDS", "VOTING", "FINISHED"].includes(state);
    let instruction = "Waiting for the storyteller to choose a card and give a clue.";
    if (state === "WAITING_STORYTELLER_CARD" && isNarrator && !readOnly) instruction = "Choose a card from your hand";
    if (state === "WAITING_STORYTELLER_CLUE" && isNarrator && !readOnly) instruction = "Give your card a clue";
    if (state === "WAITING_PLAYER_CARDS") instruction = !readOnly && !isNarrator && !turn.hasPlayed ? "Select your card" : "Waiting for other players to choose their card";
    if (tableVisible) instruction = closed ? "Round result" : readOnly || isNarrator || turn.hasVoted ? "Wait until everyone has voted" : "Vote for the storyteller’s card";
    return (
        <div className="turn-scene" key={`${turn.id}:${state}`}>
            <GameResult game={game} currentPlayer={currentPlayer} readOnly={readOnly} />
            {turn.clue != null && <p className="clue">Clue: {turn.clue}</p>}
            <h2 aria-live="polite">{instruction}</h2>
            {state === "WAITING_STORYTELLER_CLUE" && isNarrator && !readOnly && <Form className="clue-form" onSubmit={handleSendClue}>
                <Label for="clue">Clue</Label>
                <div className="clue-form-row">
                    <Input type="text" required name="clue" id="clue" value={turn.clue || ""} onChange={handleChange} disabled={busy} />
                    <Button color="success" type="submit" disabled={busy || !turn.clue?.trim()}>Set clue</Button>
                </div>
            </Form>}
            {tableVisible && <>
                {!closed && <p className="game-help">{readOnly ? "Spectator view · voting is available to participants only." : isNarrator ? "The storyteller does not vote." : turn.hasVoted ? "Your vote has been sent." : "Your own card is marked. You cannot vote for it."}</p>}
                <ul className="game-card-list table-cards">
                    {(turn.playedCards || []).map((card, index) => {
                        const canVote = !readOnly && !isNarrator && !card.isOwn && !turn.hasVoted && !closed && !busy;
                        const answer = closed && card.id === turn.answer?.id;
                        const ownVote = card.id === turn.ownVotedCardId;
                        const voteLabel = readOnly ? "Perspective vote" : "Your vote";
                        return <li key={card.id}>
                            <button type="button" className={`card-button ${canVote ? "card-clickable" : "card-disabled"} ${card.isOwn ? "card-own" : ""} ${answer ? "card-answer" : ""} ${ownVote ? "card-voted" : ""}`} disabled={!canVote} onClick={() => voteCard(card)} aria-label={ownVote ? `${voteLabel}: card ${index + 1}` : canVote ? `Vote for card ${index + 1}` : card.isOwn ? `Your card ${index + 1}` : `Card ${index + 1}`}>
                                <img className="card" src={`/Cards/${card.image.id}.png`} alt="Card" />
                                <span>{answer ? "Storyteller’s card" : card.isOwn ? "Your card" : `Card ${index + 1}`}</span>
                                {ownVote && <span className="vote-label">✓ {voteLabel}</span>}
                            </button>
                            <CardPreview card={card} label={`table card ${index + 1}`} />
                            {closed && <div className="card-result"><strong>{card.votes?.length || 0} votes</strong>{card.user?.username && <span>{card.user.username}</span>}</div>}
                        </li>;
                    })}
                </ul>
            </>}
            {!closed && <p className="game-help">Players keep their place if they leave temporarily. A turn may wait for their return.</p>}
        </div>
    );
}

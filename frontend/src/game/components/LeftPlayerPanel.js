import React from "react";
import OpponentPanel from "./OpponentPanel";

export default function LeftPlayerPanel({currentPlayer, players, getPlayerName, storyteller, getPlayerHandCards}) {
    const index = players.findIndex(player => player.id === currentPlayer?.id);
    if (index < 0 || players.length < 2) return null;
    const player = players[(index - 1 + players.length) % players.length];
    return <OpponentPanel position="left" player={player} storyteller={storyteller} getPlayerName={getPlayerName} cards={getPlayerHandCards?.(player)} />;
}

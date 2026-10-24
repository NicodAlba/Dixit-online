import React from "react";
import OpponentPanel from "./OpponentPanel";

export default function TopPlayerPanel({currentPlayer, players, getPlayerName, storyteller, getPlayerHandCards}) {
    const index = players.findIndex(player => player.id === currentPlayer?.id);
    if (index < 0 || players.length < 2 || players.length !== 4) return null;
    const player = players[(index + 2) % players.length];
    return <OpponentPanel position="top" player={player} storyteller={storyteller} getPlayerName={getPlayerName} cards={getPlayerHandCards?.(player)} />;
}

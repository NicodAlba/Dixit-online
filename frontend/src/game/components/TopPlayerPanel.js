import React from "react";
import {List, ListInlineItem} from "reactstrap";
import fairy from "../../static/images/icons/Fairy.png"
import "../../static/css/game/Game.css"
export default function TopPlayerPanel({currentPlayer, players, getPlayerName, storyteller, getPlayerHandCards}) {
        if (!currentPlayer?.id || players.length < 4) {
            return null;
        }
        const indexPlayer = players.findIndex((player) => player.id === currentPlayer.id)
        if (indexPlayer === -1) {
            return null;
        }
        const topPlayerIndex = (indexPlayer + 2) % players.length;
        const topPlayer = players[topPlayerIndex];
        const topPlayerCards = getPlayerHandCards ? getPlayerHandCards(topPlayer) : Array.from({length: topPlayer?.handSize || 0}, (_, index) => ({id: `back-${index}`}));
        const cardsList = topPlayerCards?.map((c) => {
            const image = `/Cards/card_reversed.png`
            return (
                <ListInlineItem key={c.id} className="top-card-slot">
                    <img src = {image} alt="Card" className="card" style={{width:"40px"}}/>
                </ListInlineItem>
            );
        });
        const isStoryteller = topPlayer?.id === storyteller?.id
        console.log(topPlayer)
        return (
            <div className="top-player-container player-panel" style={{display: "flex", flexDirection: "column" }}>
                <List className="opponent-info">
                    <ListInlineItem>
                        <h4 className="opponent-points">{topPlayer?.points} pts</h4>
                        {isStoryteller && 
                        <ListInlineItem>
                            <img src = {fairy} alt="st" style={{width:"30px"}}/>
                        </ListInlineItem>}
                        <ListInlineItem className="opponent-name">{getPlayerName(topPlayer)}</ListInlineItem>
                    </ListInlineItem>
                    {cardsList}
                </List>
                
            </div>
            
        )
    }

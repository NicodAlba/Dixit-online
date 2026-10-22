import React from "react";
import {Button, List, ListInlineItem} from "reactstrap";
import fairy from "../../static/images/icons/Fairy.png"
import "../../static/css/game/Game.css"
export function RenderSpectatingPlayer({currentPlayer, spectatingUser, isStoryteller, getPlayerName}) {

        const cardsList = (currentPlayer?.cards || []).map((c) => {
            const image = `/Cards/${c.image?.id}.png`
            return (
                <ListInlineItem key={c.id}>
                    <img className= "card" src = {image} alt="Card" style={{width:"50px"}}/>
                </ListInlineItem>
            );
        });
        return (
            <div className="owned-cards-container">
                <List>
                    <ListInlineItem><h4>{getPlayerName(currentPlayer) || spectatingUser?.username || ""}</h4></ListInlineItem>
                    {isStoryteller && 
                    <ListInlineItem>
                        <img src = {fairy} alt="st" style={{width:"30px"}}/>
                    </ListInlineItem>}
                    <h5>{currentPlayer?.points} pts</h5>
                </List>
                <List>{cardsList}</List>
            </div>
        )
    }
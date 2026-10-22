import { List, ListInlineItem } from "reactstrap";
import fairy from "../../static/images/icons/Fairy.png"
import "../../static/css/game/Game.css"
export default function RenderLeftPlayer({currentPlayer, players, getPlayerName, storyteller}) {
        if (!currentPlayer?.id || players.length < 2) {
            return null;
        }
        const fullGame = players.length === 4
        const playerToPut = fullGame?3:2
        const indexPlayer = players.findIndex((player) => player.id === currentPlayer.id)
        if (indexPlayer === -1) {
            return null;
        }
        const firstPlayer = indexPlayer === 0
        const previousPlayer = players[firstPlayer?playerToPut:indexPlayer-1]
        const cardsList = previousPlayer?.cards?.map((c) => {
            const image = `/Cards/card_reversed.png`
            return (
                <div key={c.id} className="opponent-card-slot">
                        <img className= "card"src = {image} alt="Card" style={{width:"30px"}}/>
                    
                </div>
            );
        });
        const isStoryteller = previousPlayer?.id === storyteller?.id
        console.log(previousPlayer)
        return (
            <div className="left-player-container player-panel">
                <List className="opponent-info">
                    <ListInlineItem className="opponent-name">{getPlayerName(previousPlayer)}</ListInlineItem>
                    {isStoryteller && 
                    <ListInlineItem>
                        <img src = {fairy} alt="st" style={{width:"50px"}}/>
                    </ListInlineItem>}
                    <h3 className="opponent-points">{previousPlayer?.points} pts</h3>
                </List>
                <div className="opponent-card-column">
                    {cardsList}
                </div>
            </div>
        )
    }
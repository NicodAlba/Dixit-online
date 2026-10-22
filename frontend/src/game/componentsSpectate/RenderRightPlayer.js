import { List, ListInlineItem } from "reactstrap";
import fairy from "../../static/images/icons/Fairy.png"
import "../../static/css/game/Game.css"
export default function RenderRightPlayer({currentPlayer, players, getPlayerName, storyteller}){
        if (!currentPlayer?.id || players.length < 2) {
            return null;
        }
        const indexPlayer = players.findIndex((player) => player.id === currentPlayer.id)
        if (indexPlayer === -1) {
            return null;
        }
        const lastPlayer = indexPlayer === players.length - 1
        const nextPlayer = players[lastPlayer?0:indexPlayer+1]
        const cardsList = nextPlayer?.cards?.map((c) => {
            const image = `/Cards/card_reversed.png`
            return (
                <div key={c.id} className="opponent-card-slot">
                    <img className= "card" src = {image} alt="Card" style={{width:"30px"}}/>
                </div>
            );
        });
        const isStoryteller = nextPlayer?.id === storyteller?.id
        return (
            <div className="right-player-container player-panel">
                <List className="opponent-info">
                    {isStoryteller && 
                    <ListInlineItem>
                        <img src = {fairy} alt="st" style={{width:"30px"}}/>
                    </ListInlineItem>}
                    <ListInlineItem className="opponent-name">{getPlayerName(nextPlayer)}</ListInlineItem>
                    <h3 className="opponent-points">{nextPlayer?.points} pts</h3>
                </List>
                <div className="opponent-card-column">
                    {cardsList}
                </div>
            </div>
        )
    }
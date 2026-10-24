import React, {useState} from "react";
import "../static/css/game/Game.css";
import tokenService from "../services/token.service";
import getIdFromUrl from "./../util/getIdFromUrl"
import { RenderSpectatingPlayer } from "./componentsSpectate/RenderSpectatingPlayer";
import RenderLeftPlayer from "./componentsSpectate/RenderLeftPlayer";
import RenderRightPlayer from "./componentsSpectate/RenderRightPlayer";
import RenderTopPlayer from "./componentsSpectate/RenderTopPlayer";
import useGameTurnState from "../hooks/useGameTurnState";
import usePlayerNames from "../hooks/usePlayerNames";
import TurnStatusPanel from "./componentsSpectate/TurnStatusPanel";
import "../static/css/game/Game.css"
import GameLayout from "./components/GameLayout";
import { Button } from "reactstrap";
import { useNavigate } from "react-router-dom";

export default function Spectate() {
    const jwt = tokenService.getLocalAccessToken();
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const gameId = getIdFromUrl(2);
    const playerId = getIdFromUrl(4);
    const {game, turn, players, storyteller, refresh, loading, connection} = useGameTurnState({gameId, jwt, setMessage, setVisible, perspectivePlayerId: playerId});

    const usernamesById = usePlayerNames(players, jwt);
    const navigate = useNavigate();

    const getPlayerName = (player) => {
        if (!player) {
            return "";
        }
        const playerUser = player.user;
        if (playerUser?.username) {
            return playerUser.username;
        }
        if (typeof playerUser === "string") {
            return playerUser;
        }
        const userId = typeof playerUser === "number" ? playerUser : playerUser?.id;
        if (userId != null) {
            return usernamesById[userId] || `Player ${userId}`;
        }
        if (player.username) {
            return player.username;
        }
        return "";
    };
    const spectatingPlayer = players.find((player) => player?.id === Number(playerId))
    const spectatingUser = spectatingPlayer?.user
    const isStoryteller = storyteller?.id  === spectatingPlayer?.id;

    return (
        <GameLayout gameId={gameId} game={game} loading={loading} connection={connection}
            storytellerName={getPlayerName(storyteller)} role={`Spectating ${getPlayerName(spectatingPlayer)}`}
            message={visible ? message : null} onDismiss={() => setVisible(false)} onRetry={refresh}
            toolbar={<>
                <label className="perspective-picker">Perspective
                    <select value={playerId} onChange={event => navigate(`/games/${gameId}/spectate/${event.target.value}`)}>
                        {players.map(player => <option key={player.id} value={player.id}>{getPlayerName(player)}</option>)}
                    </select>
                </label>
                <Button className="leave-game" onClick={() => navigate("/")}>Leave game</Button>
            </>}
            opponents={<>
                <RenderLeftPlayer currentPlayer={spectatingPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller}/>
                <RenderTopPlayer currentPlayer={spectatingPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller}/>
                <RenderRightPlayer currentPlayer={spectatingPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller}/>
            </>}
            hand={<RenderSpectatingPlayer currentPlayer={spectatingPlayer} spectatingUser={spectatingUser} isStoryteller={isStoryteller} getPlayerName={getPlayerName}/>}
        >
            <TurnStatusPanel game={game} turn={game.cancelled ? {...turn,state:"CANCELLED"} : turn} currentPlayer={spectatingPlayer}
                storyteller={storyteller} isStoryteller={isStoryteller} getPlayerName={getPlayerName}/>
        </GameLayout>
    );
}

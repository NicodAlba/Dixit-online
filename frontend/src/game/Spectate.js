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
import useFetchState from "../util/useFetchState";
import { Button } from "reactstrap";
import { useNavigate } from "react-router-dom";

const jwt = tokenService.getLocalAccessToken();

export default function Spectate() {
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const gameId = getIdFromUrl(2);
    const {game, turn, turnId, setGame, setTurn, players, storyteller} = useGameTurnState({gameId, jwt, setMessage, setVisible});
    const playerId = getIdFromUrl(4);
    console.log(playerId)

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
    console.log(spectatingPlayer)
    console.log(players)
    const isStoryteller = storyteller?.id  === spectatingPlayer?.id;

    return (
        <div className="game-page-container">
            <Button  className= "leave-game" style={{position: "absolute", top: 10, left:30}} onClick={() => navigate("/")}>Leave game</Button>
            <TurnStatusPanel game={game} turn={turn} currentPlayer={spectatingPlayer} getPlayerName={getPlayerName}/>
            <RenderSpectatingPlayer currentPlayer={spectatingPlayer} spectatingUser={spectatingUser} isStoryteller={isStoryteller} getPlayerName={getPlayerName}/>
            <RenderLeftPlayer currentPlayer={spectatingPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller}/>
            <RenderRightPlayer currentPlayer={spectatingPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller}/>
            <RenderTopPlayer currentPlayer={spectatingPlayer} players={players} getPlayerName={getPlayerName} storyteller={storyteller}/>
        </div>
        
    );
}


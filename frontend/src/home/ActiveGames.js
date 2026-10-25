import React, {useEffect, useRef, useState} from "react";
import {useNavigate} from "react-router-dom";
import {Modal, ModalBody, ModalHeader} from "reactstrap";
import {FiArrowRightCircle, FiRefreshCw} from "react-icons/fi";

const active = game => !game.endDate && !game.cancelled;

// Only mounted with a session. /my is scoped by the server to the authenticated account.
export default function ActiveGames({jwt}) {
    const [games, setGames] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [choosing, setChoosing] = useState(false);
    const [pending, setPending] = useState(false);
    const pendingRef = useRef(false);
    const navigate = useNavigate();
    const epoch = useRef(0);
    const [reload, setReload] = useState(0);
    useEffect(() => {
        let ignore = false;
        epoch.current++;
        setGames([]);
        setLoading(true);
        fetch("/api/v1/games/my", {headers: {Authorization: `Bearer ${jwt}`}})
            .then(async response => {
                if (!response.ok) throw new Error("Unable to check your active games.");
                return response.json();
            }).then(data => {if (!ignore) {setGames(data.filter(active)); setError(null);}})
            .catch(error => {if (!ignore) setError(error.message || "Unable to check your active games.");})
            .finally(() => {if (!ignore) setLoading(false);});
        return () => {ignore = true; epoch.current++;};
    }, [jwt, reload]);

    async function returnTo(game) {
        if (pendingRef.current) return;
        const requestEpoch = epoch.current;
        pendingRef.current = true; setPending(true); setError(null);
        try {
            const response = await fetch(`/api/v1/games/${game.id}`, {headers: {Authorization: `Bearer ${jwt}`}});
            if (requestEpoch !== epoch.current) return;
            if (response.status === 404) {
                setGames(current => current.filter(item => item.id !== game.id));
                throw new Error("This lobby is no longer available.");
            }
            if (!response.ok) throw new Error("Unable to return to this game. Check your session and try again.");
            const current = await response.json();
            if (requestEpoch !== epoch.current) return;
            if (!active(current)) {
                setGames(games => games.filter(item => item.id !== game.id));
                throw new Error(current.cancelled ? "This game was cancelled." : "This game has finished.");
            }
            navigate(`/games/${current.id}${current.startDate ? "/game" : ""}`);
        } catch (error) {if (requestEpoch === epoch.current) setError(error.message || "Unable to return to this game.");}
        finally {if (requestEpoch === epoch.current) {pendingRef.current = false; setPending(false);}}
    }
    return <section className="active-games" aria-label="Your active games" aria-busy={loading || pending}>
        {loading && <span className="home-secondary-status" role="status">Checking your games…</span>}
        {error && <div role="alert">{error} <button className="text-button" disabled={loading || pending} onClick={() => setReload(value => value + 1)}><FiRefreshCw aria-hidden="true" /> Retry</button></div>}
        {games.length > 0 && <button className="home-primary" disabled={pending} onClick={() => games.length === 1 ? returnTo(games[0]) : setChoosing(true)}>
            <FiArrowRightCircle aria-hidden="true" /> {pending ? "Opening game…" : "Return to game"}
        </button>}
        <Modal isOpen={choosing} toggle={() => setChoosing(false)} labelledBy="active-games-title" returnFocusAfterClose>
            <ModalHeader id="active-games-title" toggle={() => setChoosing(false)}>Choose a game</ModalHeader>
            <ModalBody>
                {error && <p role="alert">{error}</p>}
                <ul className="game-choice-list">{games.map(game => <li key={game.id}>
                    <button disabled={pending} onClick={() => returnTo(game)}>Game {game.id} · {game.startDate ? "In progress" : "Lobby"}</button>
                    <small>{game.players?.map(player => player.user?.username).filter(Boolean).join(", ")}</small>
                </li>)}</ul>
                {!games.length && <p>No active games remain.</p>}
            </ModalBody>
        </Modal>
    </section>;
}

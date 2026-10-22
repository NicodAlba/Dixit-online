import { useEffect, useState } from "react";
import { Table, Button } from "reactstrap";
import getErrorModal from "../../util/getErrorModal";
import "../../static/css/admin/History.css";
import tokenService from "../../services/token.service";

const jwt = tokenService.getLocalAccessToken();
export default function History() {
    const [message, setMessage] = useState(null);
    const [visible, setVisible] = useState(false);

    const [playingGames, setPlayingGames] = useState([]);
    const [pastGames, setPastGames] = useState([]);
    const [filter, setFilter] = useState("all");
    useEffect(() => {
        let mounted = true;
        async function loadGames() {
            try {
                const res = await fetch(`/api/v1/games/inProgress`, {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        ...(jwt ? { Authorization: `Bearer ${jwt}` } : {}),
                    },
                    credentials: "same-origin",
                });
                if (!res.ok) {
                    const text = await res.text();
                    throw new Error(`${res.status} ${res.statusText} ${text}`);
                }
                const text = await res.text();
                let data = [];
                if (text && text.trim().length > 0) {
                    try {
                        data = JSON.parse(text);
                    } catch (e) {
                        throw new Error("Invalid JSON response: " + e.message);
                    }
                } else {
                    data = [];
                }
                if (mounted) setPlayingGames(Array.isArray(data) ? data : []);
            } catch (err) {
                setMessage("Failed to fetch data: " + err.message);
                setVisible(true);
            }
        }
        async function loadPastGames() {
            try {
                const res = await fetch(`/api/v1/games/pastGames`, {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        ...(jwt ? { Authorization: `Bearer ${jwt}` } : {}),
                    },
                    credentials: "same-origin",
                });
                if (!res.ok) {
                    const text = await res.text();
                    throw new Error(`${res.status} ${res.statusText} ${text}`);
                }
                const text = await res.text();
                let data = [];
                if (text && text.trim().length > 0) {
                    try {
                        data = JSON.parse(text);
                    } catch (e) {
                        throw new Error("Invalid JSON response: " + e.message);
                    }
                } else {
                    data = [];
                }
                if (mounted) setPastGames(Array.isArray(data) ? data : []);
            } catch (err) {
                setMessage("Failed to fetch data: " + err.message);
                setVisible(true);
            }
        }
        loadGames();
        loadPastGames();
        return () => { mounted = false; };
    }, [jwt]);

    const modal = getErrorModal(setVisible, visible, message);

    const allGamesMap = new Map();

    [...playingGames, ...pastGames].forEach((g) => {
        const status = g.endDate ? "Ended" : "Playing";
        allGamesMap.set(g.id, { ...g, status });
    });

    const allGames = Array.from(allGamesMap.values());

    const sortedGames = allGames.sort(
        (a, b) => new Date(b.startDate) - new Date(a.startDate)
    );

    const filteredGames = sortedGames.filter((game) => {
        if (filter === "playing") return game.status === "Playing";
        if (filter === "past") return game.status === "Ended";
        return true;
    });


    const gamesList = filteredGames.map((game) => (
        <tr key={game.id}>
            <td>{game.id}</td>
            <td>{game.leader?.id}</td>
            <td>{new Date(game.startDate).toLocaleString()}</td>
            <td>
                {game.endDate
                    ? new Date(game.endDate).toLocaleString()
                    : "—"}
            </td>
            <td>
                <span
                    className={`status-badge ${
                        game.status === "Playing" ? "status-playing" : "status-ended"
                    }`}
                >
                    {game.status}
                </span>
            </td>
            <td>
                {game.password}
            </td>
        </tr>
    ));

    return (
        <div className="history-page">
            <h1 className="title">Game History</h1>
            {modal}
            <div className="filter-buttons">
                <Button
                    color={filter === "all" ? "primary" : "secondary"}
                    onClick={() => setFilter("all")}
                >
                    All
                </Button>
                <Button
                    color={filter === "playing" ? "success" : "secondary"}
                    onClick={() => setFilter("playing")}
                >
                    Playing
                </Button>
                <Button
                    color={filter === "past" ? "warning" : "secondary"}
                    onClick={() => setFilter("past")}
                >
                    Past
                </Button>
            </div>
            <div className="card single-table">
                <Table className="styled-table">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Leader</th>
                        <th>Start Date</th>
                        <th>End Date</th>
                        <th>Status</th>
                        <th>Password</th>
                    </tr>
                    </thead>
                    <tbody>{gamesList}</tbody>
                </Table>
            </div>
        </div>
    );
}

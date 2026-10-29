import {useEffect, useRef, useState} from "react";

// Effects compare authorized snapshots, never hold up a newer state or replay a reconnect.
export default function useGameMotion({game, connection, perspectiveId}) {
    const previous = useRef(null);
    const sequence = useRef(0);
    const [effect, setEffect] = useState(null);
    useEffect(() => {
        const turn = game.turns?.[game.turns.length - 1];
        if (connection !== "connected" || !turn?.id || game.cancelled || game.deleted || game.endDate) {
            previous.current = null; setEffect(null); return;
        }
        const next = {gameId: game.id, perspectiveId, turnId: turn.id, state: turn.state,
            vote: turn.ownVotedCardId, count: turn.playedCardCount ?? turn.playedCards?.length ?? 0,
            hand: (game.players?.find(player => player.id === perspectiveId)?.cards || []).map(card => card.id)};
        const old = previous.current;
        previous.current = next;
        if (!old || old.gameId !== next.gameId || old.perspectiveId !== perspectiveId) {setEffect(null); return;}
        const arrivingCardIds = next.hand.filter(id => !old.hand.includes(id));
        let kind = null;
        if (old.turnId !== next.turnId) kind = "new-round";
        else if (old.state !== next.state && ["SHOWING_CARDS", "VOTING"].includes(next.state)) kind = "reveal";
        else if (old.state !== next.state && next.state === "FINISHED") kind = "round-result";
        else if (next.count > old.count) kind = "card-played";
        else if (next.vote != null && old.vote !== next.vote) kind = "vote";
        else if (old.state !== next.state) kind = "phase";
        else if (arrivingCardIds.length) kind = "deal";
        if (kind) setEffect({key: ++sequence.current, kind, arrivingCardIds});
    }, [game, connection, perspectiveId]);
    useEffect(() => {
        if (!effect) return;
        const timer = setTimeout(() => setEffect(null), 1400);
        return () => clearTimeout(timer);
    }, [effect]);
    return effect;
}

import React from "react";
import CardPreview from "./CardPreview";

export default function RoundRecap({game}) {
    const turn = [...(game.turns || [])].reverse().find(item => item.state === "FINISHED" && item.answer?.image);
    if (!turn || game.cancelled || game.endDate) return null;
    return <details className="round-recap"><summary>Previous round · reveal</summary>
        <div className="round-recap-content"><img src={`/Cards/${turn.answer.image.id}.png`} alt="Previous storyteller’s card" />
            <div><strong>Storyteller’s card</strong>{turn.clue != null && <p>{turn.clue}</p>}<CardPreview card={turn.answer} label="previous storyteller’s card" /></div>
        </div>
    </details>;
}

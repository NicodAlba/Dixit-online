import React from "react";
import TurnStatusPanel from "../components/TurnStatusPanel";

export default function SpectatorStatus(props) {
    return <TurnStatusPanel {...props} readOnly />;
}

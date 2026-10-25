import React, {useEffect} from "react";
import {useSocial} from "./SocialContext";
import Home from "../home";

// Keep old bookmarks while using the same integrated panel as navigation and the board.
export default function SocialView() {
    const social = useSocial();
    const open = social?.openPanel;
    useEffect(() => {open?.();}, [open]);
    return <Home />;
}

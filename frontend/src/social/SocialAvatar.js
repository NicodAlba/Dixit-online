import React from "react";

export default function SocialAvatar({username}) {
    const initials = Array.from(username?.trim() || "?").slice(0, 2).join("").toUpperCase();
    return <span className="social-avatar" aria-hidden="true">{initials}</span>;
}

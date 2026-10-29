// Snapshot comparisons never treat initial loads or recovery after failure as new events.
export function socialChanges(previous, next, seen) {
    const candidates = [];
    for (const item of next.relationships) {
        if (item.status === "INCOMING") candidates.push({id: `request:${item.requestId}`, section: "requests", message: `${item.username} sent you a friend request.`});
        if (item.status === "FRIEND" && previous?.relationships.some(old => old.accountId === item.accountId && old.status === "OUTGOING"))
            candidates.push({id: `accepted:${item.requestId}:${item.accountId}`, section: "friends", message: `${item.username} accepted your friend request.`});
    }
    for (const item of next.invitations) candidates.push({id: `invitation:${item.id}`, section: "invitations", message: `${item.inviter} invited you to a game.`});
    const changes = candidates.filter(item => !seen.has(item.id));
    candidates.forEach(item => seen.add(item.id));
    return previous ? changes : [];
}

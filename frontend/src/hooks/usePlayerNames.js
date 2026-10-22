import { useEffect, useState } from "react";

export default function usePlayerNames(players, jwt) {
    const [usernamesById, setUsernamesById] = useState({});

    useEffect(() => {
        const idsToFetch = players
            .map((player) => {
                const playerUser = player.user;
                if (playerUser?.username) {
                    return null;
                }
                if (typeof playerUser === "number") {
                    return playerUser;
                }
                if (playerUser?.id != null && !playerUser?.username) {
                    return playerUser.id;
                }
                return null;
            })
            .filter((id) => id != null && usernamesById[id] == null);

        if (idsToFetch.length === 0) {
            return;
        }

        let ignore = false;

        const fetchUsernames = async () => {
            try {
                const results = await Promise.all(
                    idsToFetch.map(async (id) => {
                        const response = await fetch(`/api/v1/users/${id}`, {
                            headers: {
                                Authorization: `Bearer ${jwt}`,
                                Accept: "application/json",
                            },
                        });
                        if (!response.ok) {
                            console.warn(`Unable to fetch username for user ${id}`);
                            return { id, username: `Player ${id}` };
                        }
                        const data = await response.json();
                        return { id, username: data.username || `Player ${id}` };
                    })
                );

                if (!ignore) {
                    setUsernamesById((prev) => {
                        const updated = { ...prev };
                        results.forEach(({ id, username }) => {
                            updated[id] = username;
                        });
                        return updated;
                    });
                }
            } catch (error) {
                console.error("Error fetching usernames", error);
            }
        };

        fetchUsernames();

        return () => {
            ignore = true;
        };
    }, [players, jwt, usernamesById]);

    return usernamesById;
}

import React, {useEffect, useRef, useState} from "react";
import {useSocial} from "./SocialContext";

export function SearchUser() {
    const social = useSocial();
    const [text, setText] = useState("");
    const [query, setQuery] = useState("");
    const [page, setPage] = useState(0);
    const [results, setResults] = useState(null);
    const [pending, setPending] = useState(false);
    const [error, setError] = useState(null);
    const revision = useRef(0);
    useEffect(() => () => {revision.current++;}, []);
    async function search(event, requestedPage = 0, requestedQuery = text.trim()) {
        event?.preventDefault();
        if (!requestedQuery) return;
        const current = ++revision.current;
        setPending(true); setError(null);
        try {
            const data = await social.request(`/api/v1/users/players?page=${requestedPage}&size=5&username=${encodeURIComponent(requestedQuery)}`);
            if (current !== revision.current) return;
            setResults(data); setPage(requestedPage); setQuery(requestedQuery);
        } catch (error) {if (current === revision.current) setError(error.message);}
        finally {if (current === revision.current) setPending(false);}
    }
    return <section><h3>Find a friend</h3>
        <form className="social-search" onSubmit={search}><label htmlFor="friend-search">Player name</label><input id="friend-search" placeholder="Search players" value={text} onChange={event => setText(event.target.value)} /><button disabled={!text.trim() || pending}>{pending ? "Searching…" : "Search players"}</button></form>
        {error && <p role="alert" className="social-error">{error}</p>}
        {results && <><ul className="social-list">{results.content?.map(account => {
            const relationship = social.relation(account.id);
            return <li key={account.id} className="social-person"><strong>{account.username}</strong>
                {account.id === social.selfId ? <span>You</span> : relationship?.status === "FRIEND" ? <span>Already friends</span> : relationship?.status === "OUTGOING" ? <span>Request sent</span> : relationship?.status === "INCOMING" ? <button disabled={social.pending} onClick={() => social.acceptRequest(relationship.requestId)}>Accept request</button> : <button disabled={social.pending || !social.loaded} onClick={() => social.sendRequest(account.id)}>Add friend</button>}
            </li>;
        })}</ul>{!results.content?.length && <p>No players found.</p>}
        <div className="social-pagination"><button disabled={pending || page === 0} onClick={() => search(null, page - 1, query)}>Previous</button><span>Page {page + 1}</span><button disabled={pending || results.last !== false} onClick={() => search(null, page + 1, query)}>Next</button></div></>}
    </section>;
}

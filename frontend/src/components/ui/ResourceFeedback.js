import React from "react";
export default function ResourceFeedback({resource,label}) {
    if(resource.loading)return <p className="records-feedback" role="status">Loading {label}…</p>;
    if(resource.error)return <div className="records-feedback records-error" role="alert"><p>{resource.error}</p><button type="button" onClick={resource.retry}>Retry</button>{resource.status===401&&<a href="/login">Log in</a>}</div>;
    return null;
}

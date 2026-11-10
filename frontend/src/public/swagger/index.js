import {useEffect, useState} from "react";
import SwaggerUI from "swagger-ui-react";
import "swagger-ui-react/swagger-ui.css";

export default function SwaggerDocs() {
    const [revision, setRevision] = useState(0);
    const [result, setResult] = useState({loading: true, spec: null, error: null});
    useEffect(() => {
        let stopped = false;
        const controller = new AbortController();
        setResult({loading: true, spec: null, error: null});
        async function load() {
            try {
                const response = await fetch("/v3/api-docs", {headers: {Accept: "application/json"}, signal: controller.signal});
                if (!response.ok) throw new Error("API documentation is currently unavailable. Please try again.");
                const spec = await response.json();
                if (!(spec?.swagger === "2.0" || /^3\.\d+\.\d+(?:[-+].*)?$/.test(spec?.openapi || ""))) {
                    throw new Error("The server returned an invalid API definition. Please try again.");
                }
                if (!stopped) setResult({loading: false, spec, error: null});
            } catch (error) {
                if (!stopped) setResult({loading: false, spec: null, error: error.message || "Unable to load API documentation. Please try again."});
            }
        }
        load();
        return () => {stopped = true; controller.abort();};
    }, [revision]);
    return <main className="container-fluid py-4" aria-label="API documentation">
        {result.loading ? <p role="status">Loading API documentation…</p> : result.error ? <div>
            <p role="alert">{result.error}</p>
            <button type="button" onClick={() => setRevision(value => value + 1)}>Retry</button>
        </div> : <SwaggerUI spec={result.spec} />}
    </main>;
}

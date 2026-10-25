import { useEffect, useState } from "react";


/**
 * Custom React hook to fetch data from a given URL and manage the fetched data within state.
 * This hook supports JWT-based authentication, message handling, and conditional fetching.
 *
 * @param {any} initial - The initial value for the `data` state.
 * @param {string} url - The URL from which to fetch the data. If not provided, the fetch request will not be initiated.
 * @param {string|null} jwt - The JSON Web Token (JWT) for authorization. If provided, the token will be included in the request headers.
 * @param {function|null} setMessage - Function to update the message state based on the server's response. If `null`, alerts will be used instead.
 * @param {function} setVisible - Function to control the visibility of the message or alert.
 * @param {string|null} [id=null] - Optional parameter to control fetch behavior. If `id` is `"new"`, the fetch request is skipped.
 * @param {function|null} [onError=null] - Optional caller policy; return true when the error has been handled.
 * 
 * @returns {[any, function]} - Returns data, its setter, and optional request metadata:
 *   - `data`: The state variable holding the fetched data.
 *   - `setData`: A function to manually update the `data` state.
 *   - `{loading}`: Whether the current request is pending; existing two-item consumers remain compatible.
 *
 * @example
 * const [data, setData] = useFetchState({}, 'https://api.example.com/data', jwtToken, setMessage, setVisible);
 */

export default function useFetchState(initial, url, jwt, setMessage, setVisible, id = null, onError = null) {
    const [data, setData] = useState(initial);
    const [loading, setLoading] = useState(Boolean(url && id !== "new"));
    useEffect(() => {
        if (url) {
            if (!id || id !== "new") {
                let ignore = false;
                setLoading(true);
                fetch(
                    url,
                    jwt
                        ? {
                            headers: {
                                "Authorization": `Bearer ${jwt}`,
                            },
                        }
                        : {}
                )
                    .then(async (response) => {
                        const contentType = response.headers.get("content-type") || "";
                        const isJson = contentType.includes("application/json");
                        const payload = isJson ? await response.json() : await response.text();

                        if (!response.ok) {
                            const message = (payload && payload.message) || payload || "Request failed";
                            const error = new Error(typeof message === "string" ? message : "Request failed");
                            error.status = response.status;
                            throw error;
                        }

                        return payload;
                    })
                    .then((json) => {
                        if (ignore) return;
                        if (json && json.message) {
                            if (setMessage !== null) {
                                setMessage(json.message);
                                setVisible(true);
                            } else {
                                window.alert(json.message);
                            }
                        } else {
                            setData(json);
                        }
                    })
                    .catch((error) => {
                        if (ignore) return;
                        if (onError && onError(error) === true) return;
                        console.log(error);
                        if (setMessage !== null) {
                            setMessage(error.message || "Failed to fetch data");
                            setVisible(true);
                        } else {
                            window.alert(error.message || "Failed to fetch data");
                        }
                    }).finally(() => {if (!ignore) setLoading(false);});
                return () => {
                    ignore = true;
                };
            }

        }
    }, [url, id, jwt, setMessage, setVisible, onError]);
    return [data, setData, {loading}];
}

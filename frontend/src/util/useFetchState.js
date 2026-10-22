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
 * 
 * @returns {[any, function]} - Returns an array with two elements:
 *   - `data`: The state variable holding the fetched data.
 *   - `setData`: A function to manually update the `data` state.
 *
 * @example
 * const [data, setData] = useFetchState({}, 'https://api.example.com/data', jwtToken, setMessage, setVisible);
 */

export default function useFetchState(initial, url, jwt, setMessage, setVisible, id = null) {
    const [data, setData] = useState(initial);
    useEffect(() => {
        if (url) {
            if (!id || id !== "new") {
                let ignore = false;
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
                            throw new Error(typeof message === "string" ? message : "Request failed");
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
                        console.log(error);
                        if (setMessage !== null) {
                            setMessage(error.message || "Failed to fetch data");
                            setVisible(true);
                        } else {
                            window.alert(error.message || "Failed to fetch data");
                        }
                    });
                return () => {
                    ignore = true;
                };
            }

        }
    }, [url, id, jwt, setMessage, setVisible]);
    return [data, setData];
}

import React, {createContext, useContext, useEffect, useRef, useState} from "react";
import {Client} from "@stomp/stompjs";
import SockJS from "sockjs-client";

const ApplicationConnection = createContext(null);
export function PresenceProvider({jwt, children}) {
    const [connection, setConnection] = useState({state:"connecting", generation:0});
    useEffect(() => {
        let stopped=false, rejected=false;
        const client=new Client({
            webSocketFactory:()=>new SockJS("/ws"), connectHeaders:{Authorization:`Bearer ${jwt}`},
            reconnectDelay:5000, heartbeatIncoming:10000, heartbeatOutgoing:10000, heartbeatStrategy:"worker",
            onConnect:()=>{if(!stopped)setConnection(previous=>({state:"connected",generation:previous.generation+1}));},
            onWebSocketClose:()=>{if(!stopped&&!rejected)setConnection(previous=>({...previous,state:"reconnecting"}));},
            onStompError:()=>{if(!stopped){rejected=true;setConnection(previous=>({...previous,state:"error"}));client.deactivate();}},
        });
        client.activate();
        return ()=>{stopped=true;client.deactivate();};
    },[jwt]);
    return <ApplicationConnection.Provider value={{jwt,...connection}}>{children}</ApplicationConnection.Provider>;
}

// Connections are authorized REST snapshots, never inferred from a token or local player state.
export function usePresenceData(gameId=null, notify=false) {
    const session=useContext(ApplicationConnection);
    const jwt=session?.jwt, state=session?.state, generation=session?.generation;
    const [connections,setConnections]=useState(null);
    const [loading,setLoading]=useState(false);
    const [error,setError]=useState(null);
    const [notices,setNotices]=useState([]);
    const revision=useRef(0);
    useEffect(()=>{
        let stopped=false, baseline=null, controller;
        const invalidate=()=>{revision.current++;controller?.abort();};
        setConnections(null);setError(null);setNotices([]);
        if(!jwt || state!=="connected")return invalidate;
        async function refresh(){
            const current=++revision.current;
            controller?.abort();controller=new AbortController();setLoading(true);
            try {
                const response=await fetch(`/api/v1/presence${gameId==null?"":`?gameId=${encodeURIComponent(gameId)}`}`,{headers:{Authorization:`Bearer ${jwt}`},signal:controller.signal});
                const data=await response.json().catch(()=>null);
                if(stopped || current!==revision.current)return;
                if(!response.ok || !Array.isArray(data) || !data.every(item=>Number.isInteger(item.accountId)&&typeof item.online==="boolean"))
                    throw new Error(response.status===401?"Your session expired. Log in to reconnect.":"Unable to check connections. Retrying automatically.");
                const next=Object.fromEntries(data.map(item=>[item.accountId,item.online]));
                if(notify && baseline){
                    const changed=data.filter(item=>baseline[item.accountId]!==undefined && baseline[item.accountId]!==item.online)
                        .map(item=>({...item,key:`${current}:${item.accountId}`}));
                    if(changed.length)setNotices(previous=>[...previous,...changed].slice(-3));
                }
                baseline=next;setConnections(next);setError(null);
            } catch(error){
                if(stopped || current!==revision.current)return;
                baseline=null;setConnections(null);setNotices([]);
                setError(error.message==="Your session expired. Log in to reconnect."?error.message:"Unable to check connections. Retrying automatically.");
            } finally {if(!stopped && current===revision.current)setLoading(false);}
        }
        refresh();const timer=setInterval(refresh,5000);
        window.addEventListener("focus",refresh);
        return ()=>{stopped=true;invalidate();clearInterval(timer);window.removeEventListener("focus",refresh);};
    },[jwt,state,generation,gameId,notify]);
    useEffect(()=>{
        if(!notices.length)return;
        const timer=setTimeout(()=>setNotices([]),7000);
        return ()=>clearTimeout(timer);
    },[notices]);
    return {active:Boolean(session),connections:state==="connected"?connections:null,
        loading:Boolean(session)&&state!=="error"&&!error&&(state!=="connected"||loading||connections===null),
        error:state==="error"?"Connection authentication failed. Log in again.":error,
        notices,dismiss:()=>setNotices([])};
}

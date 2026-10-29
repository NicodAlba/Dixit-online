import {useEffect,useState} from "react";
import tokenService from "../services/token.service";

// Local GET lifecycle shared by statistics and achievements, never an action transport.
export default function useAuthorizedResource(path) {
    const jwt=tokenService.getLocalAccessToken();
    const scope=`${jwt||""}:${path||""}`;
    const [revision,setRevision]=useState(0);
    const [result,setResult]=useState({scope:null,data:null,loading:Boolean(jwt&&path),error:null,status:0});
    useEffect(()=>{
        let stopped=false;const controller=new AbortController();
        if(!jwt || !path){setResult({scope,data:null,loading:false,error:jwt?null:"Log in to view this information.",status:jwt?0:401});return;}
        setResult({scope,data:null,loading:true,error:null,status:0});
        async function read(){
            try {
                const response=await fetch(path,{headers:{Authorization:`Bearer ${jwt}`},signal:controller.signal});
                const data=await response.json().catch(()=>null);
                if(stopped)return;
                if(!response.ok){
                    const error=response.status===401?"Your session expired. Log in again.":response.status===403?"You do not have access to this information.":"Unable to load this information. Please try again.";
                    setResult({scope,data:null,loading:false,error,status:response.status});return;
                }
                setResult({scope,data,loading:false,error:null,status:response.status});
            }catch{if(!stopped)setResult({scope,data:null,loading:false,error:"Unable to load this information. Check your connection and try again.",status:0});}
        }
        read();return()=>{stopped=true;controller.abort();};
    },[jwt,path,scope,revision]);
    return {...(result.scope===scope?result:{data:null,loading:Boolean(jwt&&path),error:null,status:0}),retry:()=>setRevision(value=>value+1)};
}

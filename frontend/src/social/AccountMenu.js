import React, {useEffect, useId, useRef, useState} from "react";
import {Modal, ModalBody, ModalFooter, ModalHeader} from "reactstrap";
import {FiChevronDown, FiUserPlus, FiUserCheck, FiClock, FiX} from "react-icons/fi";
import Tooltip from "../components/ui/Tooltip";
import {useSocial} from "./SocialContext";
import "../static/css/social/accountMenu.css";

export default function AccountMenu({accountId, username, className=""}) {
    const social=useSocial();
    const [open,setOpen]=useState(false);
    const [feedback,setFeedback]=useState(null);
    const titleId=useId();
    const closeRef=useRef(null);
    const refresh=social?.refresh;
    useEffect(()=>{if(open && refresh)refresh();},[open,refresh]);
    if(!social || !Number.isInteger(accountId) || accountId===social.selfId)
        return <span className={className}>{username}</span>;
    const relation=social.relation(accountId);
    const state=relation?.status;
    const label=state==="FRIEND"?"Already friends":state==="OUTGOING"?"Request sent":state==="INCOMING"?"Accept request":"Add friend";
    const Icon=state==="FRIEND"?FiUserCheck:state==="OUTGOING"?FiClock:FiUserPlus;
    const disabled=social.pending||social.loading||!social.loaded||Boolean(social.error)||state==="FRIEND"||state==="OUTGOING";
    async function act(){
        closeRef.current?.focus();
        setFeedback(null);
        const accepted=state==="INCOMING";
        const succeeded=accepted?await social.acceptRequest(relation.requestId):await social.sendRequest(accountId);
        if(succeeded)setFeedback(accepted?"Friend request accepted.":"Friend request sent.");
    }
    return <>
        <Tooltip text="Player options" className="account-menu-tooltip"><button type="button" className={`account-menu-trigger ${className}`} aria-label={`${username}: player options`} aria-haspopup="dialog" aria-expanded={open} onClick={()=>{setFeedback(null);setOpen(true);}}>
            {username} <FiChevronDown aria-hidden="true" />
        </button></Tooltip>
        <Modal isOpen={open} toggle={()=>setOpen(false)} centered className="account-menu-dialog" labelledBy={titleId}>
            <ModalHeader close={<Tooltip text="Close"><button type="button" ref={closeRef} className="account-menu-close" aria-label="Close player options" onClick={()=>setOpen(false)}><FiX aria-hidden="true" /></button></Tooltip>}><span id={titleId}>{username}</span></ModalHeader>
            <ModalBody>
                <p>Share your next story with a friend.</p>
                {social.loading && <p role="status">Checking friendship…</p>}
                {social.error && <div role="alert"><p>{social.error}</p><button type="button" onClick={refresh} disabled={social.loading}>Retry</button></div>}
                {feedback && <p role="status">{feedback}</p>}
                <button type="button" className="account-friend-action" onClick={act} disabled={disabled}><Icon aria-hidden="true" /> {label}</button>
            </ModalBody>
            <ModalFooter><button type="button" onClick={()=>setOpen(false)}>Close</button></ModalFooter>
        </Modal>
    </>;
}

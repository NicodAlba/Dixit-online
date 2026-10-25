import React from "react";
import {FiX} from "react-icons/fi";
import Tooltip from "./Tooltip";
export default function ModalClose({onClick,buttonRef}) {
    return <Tooltip text="Close"><button type="button" ref={buttonRef} className="dixit-modal-close" aria-label="Close" onClick={onClick}><FiX aria-hidden="true" /></button></Tooltip>;
}

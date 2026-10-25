import React, {useId, useState} from "react";
import {Modal, ModalBody, ModalHeader} from "reactstrap";
import {FiX, FiZoomIn} from "react-icons/fi";
import Tooltip from "../../components/ui/Tooltip";

// Only receives an illustration already present in the authorized hand/table view.
export default function CardPreview({card,label}) {
    const [open,setOpen]=useState(false);
    const titleId=useId();
    if(!card?.image?.id)return null;
    return <>
        <Tooltip text="Inspect illustration" className="card-inspect-tooltip"><button type="button" className="card-inspect" aria-label={`Inspect ${label}`} aria-haspopup="dialog" onClick={()=>setOpen(true)}><FiZoomIn aria-hidden="true" /> Inspect</button></Tooltip>
        <Modal isOpen={open} toggle={()=>setOpen(false)} centered className="card-preview-dialog" labelledBy={titleId}>
            <ModalHeader close={<Tooltip text="Close"><button type="button" className="card-preview-close" aria-label="Close card preview" onClick={()=>setOpen(false)}><FiX aria-hidden="true" /></button></Tooltip>}><span id={titleId}>{label}</span></ModalHeader>
            <ModalBody><img src={`/Cards/${card.image.id}.png`} alt={`Enlarged illustration: ${label}`} /><p>Take a closer look. Close this view to play or vote.</p></ModalBody>
        </Modal>
    </>;
}

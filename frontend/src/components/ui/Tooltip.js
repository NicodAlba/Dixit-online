import React, {cloneElement, useId} from "react";

export default function Tooltip({text, children, className = ""}) {
    const id = useId();
    return <span className={`dixit-tooltip ${className}`}>
        {cloneElement(children, {"aria-describedby": id})}
        <span id={id} role="tooltip" className="dixit-tooltip-text">{text}</span>
    </span>;
}

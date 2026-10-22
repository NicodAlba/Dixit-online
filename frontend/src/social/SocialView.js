import React from "react"
import {FriendList} from "./FriendList";
import {FriendRequestList} from "./FriendRequestList";
import "../../src/static/css/social/social.css"
import {SearchUser} from "./SearchUser";

export default function SocialView() {
    return (
        <div>
            <div className="center-container">
                <div  style={{display: "flex", gap:20}}>
                    <FriendList/>
                    <FriendRequestList/>
                    <SearchUser/>
                </div>
            </div>
        </div>
    )
}

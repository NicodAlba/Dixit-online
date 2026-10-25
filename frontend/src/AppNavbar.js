import React, { useEffect, useState } from 'react';
import { Collapse, Nav, Navbar, NavbarBrand, NavbarText, NavbarToggler, NavItem, NavLink } from 'reactstrap';
import { Link } from 'react-router-dom';
import tokenService from './services/token.service';
import jwt_decode from "jwt-decode";
import {FiLogIn, FiUserPlus, FiUsers, FiAward, FiBarChart2} from "react-icons/fi";
import Tooltip from "./components/ui/Tooltip";
import logo from "../src/static/images/dixit round.png"

function AppNavbar() {
    const [roles, setRoles] = useState([]);
    const [userId, setUserId] = useState("");
    const jwt = tokenService.getLocalAccessToken();
    const [collapsed, setCollapsed] = useState(true);

    const toggleNavbar = () => setCollapsed(!collapsed);

    useEffect(() => {
        if (jwt) {
            const account = jwt_decode(jwt);
            setRoles(account.authorities);
            setUserId(account.id);
        }
    }, [jwt])

    let adminLinks = <></>;
    let ownerLinks = <></>;
    let userLinks = <></>;
    let userLogout = <></>;
    let publicLinks = <></>;

    roles.forEach((role) => {
        if (role === "ADMIN") {
            adminLinks = (
                <>
                    <NavItem>
                        <NavLink style={{ color: "white" }} tag={Link} to="/users">Users</NavLink>
                    </NavItem>
                    <NavItem>
                        <NavLink style={{ color: "white" }} tag={Link} to="/games/History">History</NavLink>
                    </NavItem>
                </>
            )
        } else {
            ownerLinks = (
                <>
                    <NavItem>
                        <NavLink style={{ color: "white" }} tag={Link} to={"/users/" + userId + "/achievements"}><FiAward aria-hidden="true" /> Achievements</NavLink>
                    </NavItem>
                    <NavItem>
                        <NavLink style={{ color: "white" }} tag={Link} to="/stats"><FiBarChart2 aria-hidden="true" /> Statistics</NavLink>
                    </NavItem>
                </>
            )
        }
    })

    if (!jwt) {
        publicLinks = (
            <>
                <NavItem>
                    <NavLink style={{ color: "white" }} id="docs" tag={Link} to="/docs">Docs</NavLink>
                </NavItem>
                <NavItem>
                    <NavLink style={{ color: "white" }} id="register" tag={Link} to="/register"><FiUserPlus aria-hidden="true" /> Create account</NavLink>
                </NavItem>
                <NavItem>
                    <NavLink style={{ color: "white" }} id="login" className="nav-login" tag={Link} to="/login"><FiLogIn aria-hidden="true" /> Log in</NavLink>
                </NavItem>
            </>
        )
    } else {
        userLinks = (
            <></>
        )
        userLogout = (
            <>
                <NavItem>
                    <NavLink style={{ color: "white" }} id="docs" tag={Link} to="/docs">Docs</NavLink>
                </NavItem>
                <NavbarText style={{ color: "white" }} className="justify-content-end" tag={Link} to={"/users/" + userId}>Your profile</NavbarText>
                <NavItem className="d-flex">
                    <NavLink style={{ color: "white" }} id="logout" tag={Link} to="/logout">Logout</NavLink>
                </NavItem>
                {!roles.includes("ADMIN") && (
                    <NavItem>
                        <Tooltip text="Friends"><NavLink style={{ color: "white" }} id="friends" tag={Link} to="/friends" aria-label="Friends"><FiUsers aria-hidden="true" size={22} /></NavLink></Tooltip>
                    </NavItem>
                )}
            </>
        )
    }

    return (
        <div>
            <Navbar dark expand="md" style={{ backgroundColor: "#6b4024" }}>
                <NavbarBrand href="/" aria-label="Dixit home">
                    <img alt="logo" src={logo} style={{ height: 40, width: 40 }} />
                </NavbarBrand>
                <NavbarToggler onClick={toggleNavbar} className="ms-2" aria-controls="main-navigation" aria-expanded={!collapsed} />
                <Collapse id="main-navigation" isOpen={!collapsed} navbar>
                    <Nav className="me-auto mb-2 mb-lg-0" navbar>
                        {userLinks}
                        {adminLinks}
                        {ownerLinks}
                    </Nav>
                    <Nav className="ms-auto mb-2 mb-lg-0" navbar>
                        {publicLinks}
                        {userLogout}
                    </Nav>
                </Collapse>
            </Navbar>
        </div>
    );
}

export default AppNavbar;

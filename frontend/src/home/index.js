import React, {useState, useEffect, useRef} from "react";
import "../App.css";
import "../static/css/home/home.css";
import logo from "../static/images/Logo dixit.webp";
import roundLogo from "../static/images/dixit round.png";
import tokenService from "../services/token.service";
import jwt_decode from "jwt-decode";
import {Link, useLocation, useNavigate} from "react-router-dom";
import {Button, Form, Input, Label, Modal, ModalHeader, ModalBody} from "reactstrap";
import {FiPlay, FiPlusCircle, FiLogIn, FiUserPlus, FiKey, FiArrowLeft} from "react-icons/fi";
import {GameInvitationList} from "./GameInvitationList";
import Tooltip from "../components/ui/Tooltip";
import ActiveGames from "./ActiveGames";
import ModalClose from "../components/ui/ModalClose";

export default function Home() {
    const jwt = tokenService.getLocalAccessToken();
    const roles = jwt ? jwt_decode(jwt).authorities : [];
    const [showOptions, setShowOptions] = useState(false);
    const [showAuth, setShowAuth] = useState(false);
    const [view, setView] = useState(null);
    const [message, setMessage] = useState(null);
    const [game, setGame] = useState({password: "", gameId: ""});
    const [pending, setPending] = useState(false);
    const pendingRef = useRef(false);
    const navigate = useNavigate();
    const location = useLocation();
    const [notice, setNotice] = useState(location.state?.notice || null);
    useEffect(() => {
        if (!location.state?.notice) return;
        setNotice(location.state.notice);
        navigate(location.pathname, {replace: true, state: {...location.state, notice: null}});
    }, [location.pathname, location.state, navigate]);

    async function submit(event) {
        event.preventDefault();
        if (pendingRef.current || !jwt) return;
        pendingRef.current = true; setPending(true); setMessage(null);
        try {
            const response = await fetch(view === "create" ? "/api/v1/games" : `/api/v1/games/${game.gameId}/join`, {
                method: view === "create" ? "POST" : "PATCH",
                headers: {Authorization: `Bearer ${jwt}`, Accept: "application/json", "Content-Type": "application/json"},
                body: JSON.stringify({password: game.password || ""}),
            });
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || (response.status === 404 ? "Game not found." : response.status === 401 ? "Incorrect password or expired session." : "Unable to open this game. Please try again."));
            navigate(`/games/${data.id}`);
        } catch (error) {setMessage(error.message || "Unable to reach the server. Please try again.");}
        finally {pendingRef.current = false; setPending(false);}
    }
    const change = event => setGame({...game, [event.target.name]: event.target.value});
    return <main className="home-page-container dixit-ui">
        <section className="hero-div">
            {notice && <div role="status" className="home-notice">{notice} <button onClick={() => setNotice(null)} aria-label="Dismiss notification">×</button></div>}
            <Tooltip text="Dixit home"><Link className="hero-logo" to="/" aria-label="Dixit home">
                <span className="hero-logo-inner"><span className="hero-logo-front"><img src={logo} alt="Dixit" /></span><span className="hero-logo-back"><img src={roundLogo} alt="" /><span>Tell your story</span></span></span>
            </Link></Tooltip>
            {roles.includes("ADMIN") ? <h1>Welcome to the admin page</h1> : <>
                <p className="home-eyebrow">A world of imagination</p>
                <h1>Do you want to play?</h1>
                {jwt && roles.includes("PLAYER") && <ActiveGames key={jwt} jwt={jwt} />}
                {!showOptions && <Button color="warning" className="home-play" onClick={() => jwt ? setShowOptions(true) : setShowAuth(true)}><FiPlay aria-hidden="true" /> Play game</Button>}
                {showOptions && !view && <div className="home-actions">
                    <Button color="warning" onClick={() => setView("create")}><FiPlusCircle aria-hidden="true" /> Create game</Button>
                    <Button color="warning" onClick={() => setView("join")}><FiKey aria-hidden="true" /> Join game</Button>
                </div>}
                {view && <Form onSubmit={submit} className="home-game-form" aria-busy={pending}>
                    {view === "join" && <><Label for="gameId">Enter the game ID:</Label><Input required min="1" type="number" name="gameId" id="gameId" placeholder="Enter game ID" value={game.gameId} onChange={change} disabled={pending} /></>}
                    <Label for="password">{view === "create" ? "Add a password to the game." : "Enter game password."}</Label>
                    <Input type="text" name="password" id="password" placeholder="Optional password" value={game.password} onChange={change} disabled={pending} />
                    {message && <p className="home-error" role="alert">{message}</p>}
                    <Button color="success" type="submit" disabled={pending}>{pending ? "Please wait…" : view === "create" ? "Create game" : "Join game"}</Button>
                </Form>}
                {showOptions && <button className="text-button" disabled={pending} onClick={() => {setView(null); setShowOptions(false); setMessage(null);}}><FiArrowLeft aria-hidden="true" /> Back</button>}
            </>}
        </section>
        <GameInvitationList />
        <Modal isOpen={showAuth} toggle={() => setShowAuth(false)} className="home-choice-dialog" labelledBy="play-auth-title" returnFocusAfterClose>
            <ModalHeader close={<ModalClose onClick={() => setShowAuth(false)} />}><span id="play-auth-title">Join the story</span></ModalHeader>
            <ModalBody><p>Log in or create an account to play.</p><div className="home-auth-choices">
                <button className="home-primary" onClick={() => navigate("/login")}><FiLogIn aria-hidden="true" /> Log in</button>
                <button className="home-primary" onClick={() => navigate("/register")}><FiUserPlus aria-hidden="true" /> Create account</button>
            </div></ModalBody>
        </Modal>
    </main>;
}

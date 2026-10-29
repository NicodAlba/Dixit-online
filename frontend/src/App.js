import React from "react";
import {Route, Routes, useLocation, matchPath} from "react-router-dom";
import jwt_decode from "jwt-decode";
import {ErrorBoundary} from "react-error-boundary";
import AppNavbar from "./AppNavbar";
import Home from "./home";
import PrivateRoute from "./privateRoute";
import RegisterPlayer from "./auth/register";
import Login from "./auth/login";
import Logout from "./auth/logout";
import PlanList from "./public/plan";
import tokenService from "./services/token.service";
import UserListAdmin from "./admin/users/UserListAdmin";
import UserEditAdmin from "./admin/users/UserEditAdmin";
import Lobby from "./game/Lobby"
import UserEditPlayer from "./admin/users/UserEditPlayer";
import SwaggerDocs from "./public/swagger";
import History from "./admin/history/History";
import Game from "./game/Game";
import {PresenceProvider} from "./presence/PresenceContext";
import {SocialProvider} from "./social/SocialContext";
import SocialView from "./social/SocialView";
import AchievementList from "./achievements/achievementList";
import StatsDashboard from "./statistics/StatsDashboard";
import Spectate from "./game/Spectate";

function ErrorFallback({ error, resetErrorBoundary }) {
  return (
    <div role="alert">
      <p>Something went wrong:</p>
      <pre>{error.message}</pre>
      <button onClick={resetErrorBoundary}>Try again</button>
    </div>
  )
}

function App() {

  const location = useLocation();

  // Definimos los patrones que queremos excluir
  const patternsToHide = [
    "/games/:id/game",
    "/games/:id/spectate/:id"
  ];


  const hideNavbar = patternsToHide.some(pattern => 
    matchPath(pattern, location.pathname)
  );

  const jwt = tokenService.getLocalAccessToken();
  let roles = []
  if (jwt) {
    roles = getRolesFromJWT(jwt);
  }

  function getRolesFromJWT(jwt) {
    return jwt_decode(jwt).authorities;
  }

  let adminRoutes = <></>;
  let playerRoutes = <></>;
  let userRoutes = <></>;
  let publicRoutes = <></>;

  roles.forEach((role) => {
    if (role === "ADMIN") {
      adminRoutes = (
        <>
          <Route path="/users" exact={true} element={<PrivateRoute><UserListAdmin /></PrivateRoute>} />
          <Route path="/users/:username" exact={true} element={<PrivateRoute><UserEditAdmin /></PrivateRoute>} />
          <Route path="/games/History" exact={true} element={<PrivateRoute><History /></PrivateRoute>} />
        </>)
    }
    if (role === "PLAYER") {
      playerRoutes = (
        <>
          <Route path="/users/:username" exact={true} element={<PrivateRoute><UserEditPlayer/></PrivateRoute>} />  
          <Route path= "/games/:gameId" exact={true} element={<PrivateRoute><Lobby/></PrivateRoute>}/>
          <Route path= "/games/:gameId/join" exact={true} element={<PrivateRoute><Lobby/></PrivateRoute>}/>
          <Route path = "/games/:gameId/game" exact={true} element={<PrivateRoute><Game/></PrivateRoute>}/>
          <Route path = "/friends" exact={true} element={<PrivateRoute><SocialView/></PrivateRoute>}/>
          <Route path = "/users/:userId/achievements" exact={true} element={<PrivateRoute><AchievementList/></PrivateRoute>}/>
          <Route path="/stats" exact={true} element={<PrivateRoute><StatsDashboard/></PrivateRoute>} />
          <Route path="/games/:gameId/spectate/:playerId" exact={true} element={<PrivateRoute><Spectate/></PrivateRoute>} />
        </>)
    }    
  })
  if (!jwt) {
    publicRoutes = (
      <>        
        <Route path="/register" element={<RegisterPlayer />} />
        <Route path="/login" element={<Login />} />
      </>
    )
  } else {
    userRoutes = (
      <>
        {/* <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} /> */}        
        <Route path="/logout" element={<Logout />} />
        <Route path="/login" element={<Login />} />
      </>
    )
  }

  const content = (
    <div>
      <ErrorBoundary FallbackComponent={ErrorFallback} >
        {!hideNavbar && <AppNavbar />}
        <Routes>
          <Route path="/" exact={true} element={<Home />} />
          <Route path="/plans" element={<PlanList />} />
          <Route path="/docs" element={<SwaggerDocs />} />
          {publicRoutes}
          {userRoutes}
          {adminRoutes}
          {playerRoutes}
        </Routes>
      </ErrorBoundary>
    </div>
  );
  const accountContent = jwt && roles.includes("PLAYER") ? <SocialProvider key={jwt} jwt={jwt}>{content}</SocialProvider> : content;
  return jwt ? <PresenceProvider key={jwt} jwt={jwt}>{accountContent}</PresenceProvider> : accountContent;
}

export default App;

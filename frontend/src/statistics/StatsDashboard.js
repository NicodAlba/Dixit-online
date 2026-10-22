import React, { useEffect, useState } from 'react';
import tokenService from '../services/token.service';
import { Nav, NavItem, NavLink, TabContent, TabPane } from 'reactstrap';
import GlobalStats from './GlobalStats';
import UserStats from './UserStats';
import Rankings from './Rankings';
import './Stats.css';

export default function StatsDashboard() {

    const [role, setRole] = useState(null);
    const [userId, setUserId] = useState(null);
    const [activeTab, setActiveTab] = useState('global');
    const jwt = tokenService.getLocalAccessToken();

    useEffect(() => {
        if (jwt) {
            const decodedToken = JSON.parse(atob(jwt.split('.')[1]));
            setRole(decodedToken.authorities);
            setUserId(decodedToken.id);
        }
    }, [jwt]);

    const toggleTab = (tab) => {
        if (activeTab !== tab) setActiveTab(tab);
    };

    return (
        <div className="dashboard-container">
            <div className="centered-content">
                <h1 className="stats-header">Statistics Dashboard</h1>
                
                <Nav tabs className="custom-nav">
                    <NavItem>
                        <NavLink
                            className={`custom-nav-link ${activeTab === 'global' ? 'active' : ''}`}
                            onClick={() => toggleTab('global')}
                        >
                            Global Statistics
                        </NavLink>
                    </NavItem>
                    <NavItem>
                        <NavLink
                            className={`custom-nav-link ${activeTab === 'rankings' ? 'active' : ''}`}
                            onClick={() => toggleTab('rankings')}
                        >
                            Rankings
                        </NavLink>
                    </NavItem>
                    {Array.isArray(role) && role.includes('PLAYER') && (
                        <NavItem>
                            <NavLink
                                className={`custom-nav-link ${activeTab === 'my-stats' ? 'active' : ''}`}
                                onClick={() => toggleTab('my-stats')}
                            >
                                My Statistics
                            </NavLink>
                        </NavItem>
                    )}
                </Nav>

                <TabContent activeTab={activeTab}>
                    <TabPane tabId="global">
                        <GlobalStats />
                    </TabPane>
                    <TabPane tabId="rankings">
                        <Rankings />
                    </TabPane>
                    {Array.isArray(role) && role.includes('PLAYER') && (
                        <TabPane tabId="my-stats">
                            <UserStats userId={userId} />
                        </TabPane>
                    )}
                </TabContent>
            </div>
        </div>
    );
}
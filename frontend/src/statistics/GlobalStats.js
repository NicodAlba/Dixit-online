import { useEffect, useState } from 'react';
import tokenService from '../services/token.service';
import { Card, CardBody, CardTitle, CardText, Row, Col, Spinner } from 'reactstrap';

export default function GlobalStats() {
    const [globalStats, setGlobalStats] = useState(null);
    
    useEffect(() => {
        const jwt = tokenService.getLocalAccessToken();
        fetch('/api/v1/stats/global', {
            headers: {
                'Authorization': `Bearer ${jwt}`,
                'Content-Type': 'application/json',
            },
        })
        .then(response => response.json())
        .then(data => setGlobalStats(data))
        .catch(error => console.error(error));
    }, []);

    if (!globalStats) return <div className="text-center mt-5"><Spinner color="secondary" /></div>;

    const StatCard = ({ title, value, unit, colSize = "3" }) => (
        <Col md={colSize} sm="6" className="mb-4 d-flex align-items-stretch justify-content-center">
            <Card className="stats-card w-100">
                <CardBody className="stats-card-body">
                    <CardTitle className="stats-card-title">{title}</CardTitle>
                    <CardText className="stats-card-value">
                        {value}
                        {unit && <span className="stats-unit">{unit}</span>}
                    </CardText>
                </CardBody>
            </Card>
        </Col>
    );

    return (
        <div>
            <h4 className="section-title">Games Overview</h4>
            <Row className="justify-content-center">
                <StatCard title="Total Games Played" value={globalStats.totalGamesPlayed ?? 0} />
                <StatCard title="Average Games/User" value={(globalStats.averageGamesPerUser ?? 0).toFixed(2)} />
                <StatCard title="Max Games By User" value={globalStats.maxGamesByUser ?? 0} />
                <StatCard title="Min Games By User" value={globalStats.minGamesByUser ?? 0} />
            </Row>

            <h4 className="section-title">Community & Players</h4>
            <Row className="justify-content-center">
                <StatCard title="Total Users Played" value={globalStats.totalUsersThatHavePlayed ?? 0} colSize="4" />
                <StatCard title="Current Users Playing" value={globalStats.currentUsersPlaying ?? 0} colSize="4" />
                <StatCard title="Avg Players Per Game" value={(globalStats.averagePlayersPerGame ?? 0).toFixed(2)} colSize="4" />
                <StatCard title="Max Players In A Game" value={globalStats.maxPlayersInGame ?? 0} colSize="4" />
                <StatCard title="Min Players In A Game" value={globalStats.minPlayersInGame ?? 0} colSize="4" />
            </Row>

            <h4 className="section-title">Time Records</h4>
            <Row className="justify-content-center">
                <StatCard title="Total Time Played" value={globalStats.totalDuration ?? 0} unit="min" />
                <StatCard title="Avg Game Duration" value={(globalStats.averageDuration ?? 0).toFixed(2)} unit="min" />
                <StatCard title="Longest Game" value={globalStats.maxDuration ?? 0} unit="min" />
                <StatCard title="Shortest Game" value={globalStats.minDuration ?? 0} unit="min" />
            </Row>
        </div>
    );
}
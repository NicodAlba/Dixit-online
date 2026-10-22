import { useEffect, useState } from 'react';
import tokenService from '../services/token.service';
import { Card, CardBody, CardTitle, CardText, Row, Col, Spinner } from 'reactstrap';

export default function UserStats({userId}) {
    const [userStats, setUserStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (userId) {
            const jwt = tokenService.getLocalAccessToken();
            fetch(`/api/v1/stats/user/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${jwt}`,
                    'Content-Type': 'application/json',
                },
            })
            .then(response => response.json())
            .then(data => {
                // DEBUG: Mira en la consola del navegador cómo se llama la propiedad real
                console.log("Datos de estadísticas recibidos:", data);
                setUserStats(data);
                setLoading(false);
            })
            .catch(error => console.error(error));
        }
    }, [userId]);

    if (loading) return <div className="text-center mt-5"><Spinner color="secondary" /></div>;
    if (!userStats) return <div className="text-center mt-5">No statistics available.</div>;

    // Helper para determinar la ruta de la imagen con seguridad
    const getCardImage = (card) => {
        if (!card) return null;
        // Intenta 'path', luego 'imagePath', luego 'url', o una cadena vacía
        return card.path || card.imagePath || card.url || "";
    };

    const StatCard = ({ title, value, unit, color, colSize="4" }) => (
        <Col md={colSize} sm="6" className="mb-4 d-flex align-items-stretch justify-content-center">
            <Card className="stats-card w-100">
                <CardBody className="stats-card-body">
                    <CardTitle className="stats-card-title">{title}</CardTitle>
                    <CardText className="stats-card-value" style={color ? {color} : {}}>
                        {value}
                        {unit && <span className="stats-unit">{unit}</span>}
                    </CardText>
                </CardBody>
            </Card>
        </Col>
    );

    return (
        <div>
            <h4 className="section-title">General Performance</h4>
            <Row className="justify-content-center">
                <StatCard title="Total Games Played" value={userStats.gamesPlayed ?? 0} />
                <StatCard title="Games Won" value={userStats.gamesWon ?? 0} />
                <StatCard title="Win Rate" value={((userStats.winRate ?? 0) * 100).toFixed(2)} unit="%" color="#1abc9c" />
            </Row>

            <h4 className="section-title">Scoring</h4>
            <Row className="justify-content-center">
                <StatCard title="Total Points" value={userStats.totalPoints ?? 0} colSize="5" />
                <StatCard title="Average Points Per Game" value={(userStats.averagePointsPerGame ?? 0).toFixed(2)} colSize="5" />
            </Row>

            <h4 className="section-title">Strategy & Favourites</h4>
            <Row className="justify-content-center">
                <StatCard title="Deceptions Made" value={userStats.trapVotesReceived ?? 0} />
                <StatCard title="Deception Rate" value={((userStats.deceptionRate ?? 0) * 100).toFixed(2)} unit="%" />
                
                <Col md="4" className="mb-4 d-flex align-items-stretch justify-content-center">
                    <Card className="stats-card w-100">
                        <CardBody className="stats-card-body p-2">
                            <CardTitle className="stats-card-title mb-3">Favourite Card</CardTitle>
                            {userStats.favouriteCard ? (
                                <div className="favourite-card-container">
                                    <img 
                                        src={getCardImage(userStats.favouriteCard)} 
                                        alt="Card"
                                        className="favourite-card-img"
                                        onError={(e) => {
                                            e.target.onerror = null; 
                                            e.target.style.display = 'none'; // Oculta la imagen si falla
                                            e.target.parentElement.innerHTML = '<span class="text-muted small">Image not found</span>';
                                        }}
                                    />
                                </div>
                            ) : <CardText className="text-muted mt-4">No data</CardText>}
                        </CardBody>
                    </Card>
                </Col>
            </Row>

            <h4 className="section-title">Time Analysis</h4>
            <Row className="justify-content-center">
                <StatCard title="Total Time Played" value={userStats.totalDuration ?? 0} unit="min" />
                <StatCard title="Avg Game Duration" value={(userStats.averageDuration ?? 0).toFixed(2)} unit="min" />
                <StatCard title="Longest Game" value={userStats.maxDuration ?? 0} unit="min" />
                <StatCard title="Shortest Game" value={userStats.minDuration ?? 0} unit="min" />
            </Row>
        </div>
    );
}
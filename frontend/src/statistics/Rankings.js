import { Row, Col, Table, FormGroup, Label, Input, Spinner } from 'reactstrap';
import { useState, useEffect } from 'react';
import tokenService from '../services/token.service';

export default function Rankings() {
    const [rankingData, setRankingData] = useState([]);
    const [type, setType] = useState('wins');
    const [loading, setLoading] = useState(true);

    const allOptions = [
        {value: 'wins', label: 'Wins'},
        {value: 'points', label: 'Total Points'},
        {value: 'avg-points', label: 'Average Points'},
        {value: 'trap-votes', label: 'Trap Votes'},
        {value: 'win-rate', label: 'Win Rate'},
        {value: 'deception-rate', label: 'Deception Rate'},
    ];

    const currentOption = allOptions.find(o => o.value === type);

    useEffect(() => {
        const jwt = tokenService.getLocalAccessToken();
        setLoading(true);
        fetch(`/api/v1/stats/rankings/${type}`, {
            headers: {
                Authorization: `Bearer ${jwt}`,
                'Content-Type': 'application/json',
            },
        })
        .then(response => response.json())
        .then(data => {
            setRankingData(data);
            setLoading(false);
        })
        .catch(error => console.error(error));
    }, [type]);
    
    return (
        <div>
            <Row className="align-items-center mb-4 justify-content-center">
                <Col md="10" className="d-flex justify-content-between align-items-center flex-wrap">
                    <h3 className="section-title m-0 border-0 text-start" style={{color: '#2c3e50'}}>
                        <span className="me-2">🏆</span> 
                        Ranking: <span style={{color: '#e67e22'}}>{currentOption?.label}</span>
                    </h3>
                    
                    <FormGroup className="d-flex align-items-center mb-0 mt-2 mt-md-0">
                        <Label for="rankingSelector" className="me-3 mb-0 fw-bold" style={{whiteSpace: 'nowrap', color: '#95a5a6'}}>
                            CHANGE VIEW:
                        </Label>
                        <Input 
                            type="select" 
                            id="rankingSelector" 
                            value="" 
                            onChange={(e) => setType(e.target.value)}
                            className="shadow-sm"
                            style={{ borderRadius: '20px', maxWidth: '200px', border: '1px solid #bdc3c7', cursor: 'pointer', color: '#2c3e50' }}
                        >
                            <option value="" disabled>Select other ranking...</option>
                            {allOptions
                                .filter(option => option.value !== type)
                                .map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))
                            }
                        </Input>
                    </FormGroup>
                </Col>
            </Row>

            {loading ? (
                <div className="text-center py-5"><Spinner color="secondary" /></div>
            ) : (
                <Row className="justify-content-center">
                    <Col md="10">
                        <Table responsive hover className="ranking-table">
                            <thead>
                                <tr>
                                    <th style={{width: '15%'}}>Rank</th>
                                    <th style={{width: '55%'}}>User</th>
                                    <th style={{width: '30%'}}>Value</th>
                                </tr>
                            </thead>
                            <tbody>
                                {rankingData.map((row, index) => (
                                    <tr key={index}>
                                        <td>
                                            {index === 0 ? "🥇" : index === 1 ? "🥈" : index === 2 ? "🥉" : `#${index + 1}`}
                                        </td>
                                        <td>{row.username}</td>
                                        {/* Color Naranja Quemado */}
                                        <td style={{color: '#d35400', fontWeight: '800', fontSize: '1.1rem'}}>
                                            {type.includes('rate') ? `${(row.value * 100).toFixed(1)}%` : row.value}
                                        </td>
                                    </tr>
                                ))}
                                {rankingData.length === 0 && (
                                    <tr>
                                        <td colSpan="3" className="text-center text-muted py-4">
                                            No data available.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </Table>
                    </Col>
                </Row>
            )}
        </div>
    );
}
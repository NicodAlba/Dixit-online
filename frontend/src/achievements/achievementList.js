import { Table } from "reactstrap"
import useFetchState from "../util/useFetchState";
import tokenService from "../services/token.service";
import { useState } from "react";
import getIdFromUrl from "../util/getIdFromUrl";
import "../static/css/Achievements/achievements.css"
import locked from "../static/images/Achievements/Locked.png"

const jwt = tokenService.getLocalAccessToken();

export default function AchievementList() {
    const userId = getIdFromUrl(2)
    const [message, setMessage] = useState(null)
    const [visible, setVisible] = useState(false)
    const [achievements, setAchievements] = useFetchState([], `/api/v1/users/${userId}/achievements`, jwt, setMessage, setVisible);

    const achievementLocked =
        achievements.filter((a) => a.dateObtained == null).map((a) => {
            return (
                <tr key={a.id}>
                    <td className="text-center">
                        <img src={locked} alt={a.name} width="100px" />
                    </td>
                    <td className="text-center"> {a.name}</td>
                    <td className="text-center"> {a.description}</td>
                    <td className="text-center"> {a.threshold}</td>
                </tr>
            );
        });

    const achievementsCompleted =
        achievements.filter((a) => a.dateObtained != null).map((a) => {
            return (
                <tr key={a.id}>
                    <td className="text-center">
                        <img src={a.badgeImage.url} alt={a.name} width="100px" />
                    </td>
                    <td className="text-center"> {a.name}</td>
                    <td className="text-center"> {a.description}</td>
                    <td className="text-center"> {a.threshold}</td>
                    <td className="text-center"> {new Date(a.dateObtained).toLocaleDateString()}</td>
                </tr>
            );
        });
    console.log(achievementsCompleted)
    console.log(achievementLocked)
    return (
        <div className="background">
            <div className="list-achievements">
                <div>
                    <h1 className="text-center">Completed</h1>
                    <div>
                        {achievementsCompleted.length === 0 ? <h5>You haven't completed any achievement yet</h5> :
                            <Table borderless aria-label="Completed" className="completed" style={{ tableLayout: "fixed" }}>
                                <thead>
                                    <tr>
                                        <th className="head" style={{ width: "10%" }}></th>
                                        <th className="head" style={{ width: "35%" }}>Name</th>
                                        <th className="head" style={{ width: "30%" }}>Description</th>
                                        <th className="head" style={{ width: "10%" }}>Threshold</th>
                                        <th className="head" style={{ width: "15%" }}>Date Obtained</th>
                                    </tr>
                                </thead>
                                <tbody className="align-middle">{achievementsCompleted}</tbody>
                            </Table>}
                    </div>
                </div>
                <div>
                    <h1 className="text-center">Locked</h1>
                    <div>
                        {achievementLocked.length === 0 ? <h5>You have completed all achievements!</h5> :
                            <Table borderless aria-label="Locked" className="completed" style={{ tableLayout: "fixed" }}>
                                <thead>
                                    <tr>
                                        <th className="head" style={{ width: "15%" }}></th>
                                        <th className="head" style={{ width: "35%" }}>Name</th>
                                        <th className="head" style={{ width: "35%" }}>Description</th>
                                        <th className="head" style={{ width: "15%" }}>Threshold</th>
                                    </tr>
                                </thead>
                                <tbody className="align-middle">{achievementLocked}</tbody>
                            </Table>}
                    </div>
                </div>
            </div>
        </div>

    );
}
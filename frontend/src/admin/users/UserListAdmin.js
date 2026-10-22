import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Button, ButtonGroup, Table } from "reactstrap";
import { FaPenFancy, FaTrashAlt, FaChevronLeft, FaChevronRight, FaFeather } from "react-icons/fa";
import tokenService from "../../services/token.service";
import "../../static/css/admin/adminPage.css";
import deleteFromList from "../../util/deleteFromList";
import getErrorModal from "../../util/getErrorModal";
import useFetchState from "../../util/useFetchState";

const jwt = tokenService.getLocalAccessToken();

export default function UserListAdmin() {
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [users, setUsers] = useFetchState(
    [],
    `/api/v1/users`,
    jwt,
    setMessage,
    setVisible
  );
  const [alerts, setAlerts] = useState([]);

  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  const totalUsers = (users || []).length;
  const totalPages = Math.max(1, Math.ceil(totalUsers / pageSize));
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedUsers = (users || []).slice(startIndex, endIndex);

  const userList = paginatedUsers.map((user) => {
    return (
      <tr key={user.id}>
        <td>{user.username}</td>
        <td>{user?.authority?.authority}</td>
        <td>
          <ButtonGroup>
            <Button
              size="sm"
              color="secondary"
              aria-label={"edit-" + user.id}
              className="btn-action btn-edit"
              tag={Link}
              to={"/users/" + user.id}
            >
              <FaFeather className="icon" />
            </Button>
            <Button
              size="sm"
              color="secondary"
              aria-label={"delete-" + user.id}
              className="btn-action btn-delete"
              onClick={() =>
                deleteFromList(
                  `/api/v1/users/${user.id}`,
                  user.id,
                  [users, setUsers],
                  [alerts, setAlerts],
                  setMessage,
                  setVisible
                )
              }
            >
              <FaTrashAlt className="icon" />
            </Button>
          </ButtonGroup>
        </td>
      </tr>
    );
  });
  useEffect(() => {
    const computedTotalPages = Math.max(1, Math.ceil(((users || []).length) / pageSize));
    if (currentPage > computedTotalPages) {
      setCurrentPage(computedTotalPages);
    }
  }, [users, currentPage, pageSize]);

  const modal = getErrorModal(setVisible, visible, message);
  return (
    <div className="admin-page-container">
      <div className="admin-header">
        <h1 className="text-center">Users</h1>
        <Button className="add-user-button" color="secondary" tag={Link} to="/users/new" aria-label="add-user">
          <FaFeather className="add-icon" /> Add User
        </Button>
      </div>
      {alerts.map((a) => a.alert)}
      {modal}
      <div className="admin-list-card">
        <Table aria-label="users" className="mt-4 users-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Authority</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>{userList}</tbody>
        </Table>
        <div className="pagination-container">
          <Button className="pagination-nav" size="sm" color="secondary" aria-label="previous" disabled={currentPage === 1} onClick={() => setCurrentPage(currentPage - 1)}>
            <FaChevronLeft />
          </Button>
          {Array.from({ length: totalPages }, (_, i) => (
            <Button key={i + 1} className={`pagination-button ${currentPage === i + 1 ? 'active' : ''}`} size="sm" color={currentPage === i + 1 ? "primary" : "secondary"} onClick={() => setCurrentPage(i + 1)} aria-label={`page-${i+1}`}>
              {i + 1}
            </Button>
          ))}
          <Button className="pagination-nav" size="sm" color="secondary" aria-label="next" disabled={currentPage === totalPages} onClick={() => setCurrentPage(currentPage + 1)}>
            <FaChevronRight />
          </Button>
        </div>
        <div className="pagination-info">
          Showing {totalUsers === 0 ? 0 : startIndex + 1} - {Math.min(totalUsers, endIndex)} of {totalUsers}
        </div>
      </div>
    </div>
  );
}

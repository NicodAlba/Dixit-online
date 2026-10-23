import { useState } from "react";
import { Link } from "react-router-dom";
import { Form, Input, Label } from "reactstrap";
import tokenService from "../../services/token.service";
import "../../static/css/admin/adminPage.css";
import getErrorModal from "../../util/getErrorModal";
import getIdFromUrl from "../../util/getIdFromUrl";
import useFetchData from "../../util/useFetchData";
import useFetchState from "../../util/useFetchState";

const jwt = tokenService.getLocalAccessToken();

export default function UserEditAdmin() {
  const emptyItem = {
    id: null,
    username: "",
    password: "",
    authority: null,
  };
  const id = getIdFromUrl(2);
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [user, setUser] = useFetchState(
    emptyItem,
    `/api/v1/users/${id}`,
    jwt,
    setMessage,
    setVisible,
    id
  );
  const auths = useFetchData(`/api/v1/users/authorities`, jwt);
  const [password, setPassword] = useState("");

  function handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    if (name === "authority") {
      const auth = auths.find((a) => a.id === Number(value));
      setUser({ ...user, authority: auth });
    } else if (name === "password") {
      setPassword(value);
    } else setUser({ ...user, [name]: value });
  }

  function handleSubmit(event) {
    event.preventDefault();

    const payload = {
      id: user.id,
      username: user.username,
      authority: user.authority,
    };
    if (password && (!user.id || user.id === tokenService.getUser()?.id)) payload.password = password;

    fetch("/api/v1/users" + (user.id ? "/" + user.id : ""), {
      method: user.id ? "PUT" : "POST",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    })
      .then((response) => response.json())
      .then((json) => {
        if (json.message) {
          setMessage(json.message);
          setVisible(true);
        } else window.location.href = "/users";
      })
      .catch((message) => alert(message));
  }

  const modal = getErrorModal(setVisible, visible, message);
  const authOptions = auths.map((auth) => (
    <option key={auth.id} value={auth.id}>
      {auth.authority}
    </option>
  ));

  return (
    <div className="auth-page-container">
      <div className="form-header">
        <h2 className="text-center">{user.id ? "Edit User" : "Add User"}</h2>
      </div>
      {modal}
      <Link to="/users" className="back-link-outside">← Back to list</Link>
      <div className="admin-list-card auth-form-card">
        <div className="auth-form-container">
          <Form onSubmit={handleSubmit}>
            <div className="custom-form-input">
              <Label for="username" className="custom-form-input-label">
                Username
              </Label>
              <Input
                type="text"
                required
                name="username"
                id="username"
                value={user.username || ""}
                onChange={handleChange}
                className="custom-input"
              />
            </div>
            {(!user.id || user.id === tokenService.getUser()?.id) && <div className="custom-form-input">
              <Label for="password" className="custom-form-input-label">
                Password
              </Label>
              <Input
                type="password"
                required={!user.id}
                name="password"
                id="password"
                value={password}
                onChange={handleChange}
                className="custom-input"
                placeholder={user.id ? "Leave blank to keep current password" : ""}
              />
            </div>}
            <div className="custom-form-input">
              <Label for="authority" className="custom-form-input-label">
                Authority
              </Label>
              {user.id ? (
                <Input
                  type="select"
                  name="authority"
                  id="authority"
                  value={user.authority?.id || ""}
                  onChange={handleChange}
                  className="custom-input"
                >
                  <option value="">None</option>
                  {authOptions}
                </Input>
              ) : (
                <Input
                  type="select"
                  required
                  name="authority"
                  id="authority"
                  value={user.authority?.id || ""}
                  onChange={handleChange}
                  className="custom-input"
                >
                  <option value="">None</option>
                  {authOptions}
                </Input>
              )}
            </div>
            <div className="custom-button-row">
              <button className="auth-button">Save</button>
              <Link
                to={`/users`}
                className="auth-button auth-button-secondary"
                style={{ textDecoration: "none" }}
              >
                Cancel
              </Link>
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
}

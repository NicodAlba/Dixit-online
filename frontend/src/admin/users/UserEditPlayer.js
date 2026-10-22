import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Form, Input, Label } from "reactstrap";
import tokenService from "../../services/token.service";
import "../../static/css/admin/adminPage.css";
import getErrorModal from "../../util/getErrorModal";
import getIdFromUrl from "../../util/getIdFromUrl";
import useFetchState from "../../util/useFetchState";

const jwt = tokenService.getLocalAccessToken();

export default function UserEditPlayer() {
  const id = getIdFromUrl(2);
  const emptyItem = {
    id: id,
    username: "",
    password: "",
    authority: null,
  };

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
  const modal = getErrorModal(setVisible, visible, message);
  const navigate = useNavigate();
  const [password, setPassword] = useState("");

  function handleSubmit(event) {
    event.preventDefault();

    const payload = {
      id: user.id,
      username: user.username,
      authority: user.authority,
    };
    if (password) payload.password = password;

    fetch("/api/v1/users" + (user.id ? "/" + user.id : ""), {
      method: user.id ? "PUT" : "POST",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    })
      .then((response) => response.text())
      .then((data) => {
        if (data === "") {
          window.location.href = "/";
        } else {
          let json = JSON.parse(data);
          if (json.message) {
            setMessage(json.message);
            setVisible(true);
          } else {
            window.location.href = "/";
          }
        }
      })
      .catch((message) => alert(message));
  }

  function handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    if (name === "password") setPassword(value);
    else setUser({ ...user, [name]: value });

  }

  return (
    <div className="auth-page-container">
      <div className="form-header">
        <h2 className="text-center">Edit your profile</h2>
      </div>
      <Link to="/" className="back-link-outside">← Back to home</Link>
      <div className="admin-list-card auth-form-card">
        <div className="auth-form-container">
          {modal}
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

            <div className="custom-form-input">
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

            </div>

            <div className="custom-button-row">
              <button className="auth-button">Save</button>
              <Link
                to="/"
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

import React, {useRef, useState} from "react";
import {Alert} from "reactstrap";
import FormGenerator from "../../components/formGenerator/formGenerator";
import tokenService from "../../services/token.service";
import "../../static/css/auth/authButton.css";
import {loginFormInputs} from "./form/loginFormInputs";

export default function Login() {
  const [message, setMessage] = useState(null)
  const loginFormRef = useRef();
  const submitting = useRef(false);
  const [pending, setPending] = useState(false);
  

  async function handleSubmit({ values }) {

    if (submitting.current) return;
    submitting.current = true;
    setPending(true);
    const reqBody = values;
    setMessage(null);
    await fetch("/api/v1/auth/signin", {
      headers: { "Content-Type": "application/json" },
      method: "POST",
      body: JSON.stringify(reqBody),
    })
      .then(function (response) {
        if (response.status === 200) return response.json();
        else return Promise.reject("Invalid login attempt");
      })
      .then(function (data) {
        tokenService.setUser(data);
        tokenService.updateLocalAccessToken(data.token);
        window.location.href = "/";
      })
      .catch((error) => {         
        setMessage(typeof error === "string" ? error : error.message || "Unable to log in. Please try again.");
      }).finally(() => {
        submitting.current = false;
        setPending(false);
      });            
  }

  
    return (
      <div className="auth-page-container">
        {message ? (
          <Alert color="primary">{message}</Alert>
        ) : (
          <></>
        )}

        <h1>Log in</h1>

        <div className="auth-form-container login-card">
          <FormGenerator
            ref={loginFormRef}
            inputs={loginFormInputs}
            onSubmit={handleSubmit}
            numberOfColumns={1}
            listenEnterKey
            disabled={pending}
            buttonText={pending ? "Logging in…" : "Log in"}
            buttonClassName="auth-button"
          />
        </div>
      </div>
    );  
}

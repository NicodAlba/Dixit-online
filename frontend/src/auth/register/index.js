import "../../static/css/auth/authButton.css";
import "../../static/css/auth/authPage.css";
import tokenService from "../../services/token.service";
import FormGenerator from "../../components/formGenerator/formGenerator";
import {registerFormPlayerInputs} from "./form/registerFormPlayerInputs";
import {useRef, useState} from "react";

export default function RegisterPlayer() {
  const registerFormRef = useRef();
  const submitting = useRef(false);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState(null);
  const [accountCreated, setAccountCreated] = useState(false);

  async function handleSubmit({values}) {
    if (submitting.current || !registerFormRef.current.validate()) return;
    submitting.current = true;
    setPending(true);
    setMessage(null);
    let created = accountCreated;
    try {
      if (!created) {
        const response = await fetch("/api/v1/auth/signup", {
          headers: {"Content-Type": "application/json"},
          method: "POST",
          body: JSON.stringify({...values, authority: "PLAYER"}),
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || "Unable to create your account. Please try again.");
        created = true;
        setAccountCreated(true);
      }
      const response = await fetch("/api/v1/auth/signin", {
        headers: {"Content-Type": "application/json"},
        method: "POST",
        body: JSON.stringify({username: values.username, password: values.password}),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || !data.token) throw new Error("Automatic login failed. Check your password and try again, or open Log in.");
      // Establish the complete session before Home can request authorized data.
      tokenService.setUser(data);
      tokenService.updateLocalAccessToken(data.token);
      window.location.assign("/");
    } catch (error) {
      setMessage(created
        ? "Your account was created. " + (error.message || "Unable to log in. Please try again.")
        : error.message || "Unable to reach the server. Please try again.");
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <div className="auth-page-container">
      <h1>Create account</h1>
      <div className="auth-form-container login-card" aria-busy={pending}>
        {message && <p role="alert">{message}</p>}
        {accountCreated && !message && <p role="status">Account created. Logging in…</p>}
        <FormGenerator ref={registerFormRef}
          inputs={registerFormPlayerInputs.map(input => ({...input, disabled: accountCreated && input.name === "username"}))}
          onSubmit={handleSubmit} numberOfColumns={1} listenEnterKey disabled={pending}
          buttonText={pending ? "Please wait…" : accountCreated ? "Try logging in" : "Create account"}
          buttonClassName="auth-button" />
        {accountCreated && message && <a href="/login">Log in</a>}
      </div>
    </div>
  );
}

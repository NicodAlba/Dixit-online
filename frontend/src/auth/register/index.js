import "../../static/css/auth/authButton.css";
import "../../static/css/auth/authPage.css";
import tokenService from "../../services/token.service";
import FormGenerator from "../../components/formGenerator/formGenerator";
import {registerFormPlayerInputs} from "./form/registerFormPlayerInputs";
import {useRef} from "react";

export default function RegisterPlayer() {

  const registerFormRef = useRef();

  async function handleSubmit({ values }) {
    if (!registerFormRef.current.validate()) return;

    try {
      const request = { ...values, authority: "PLAYER" };

      const signupResponse = await fetch("/api/v1/auth/signup", {
        headers: { "Content-Type": "application/json" },
        method: "POST",
        body: JSON.stringify(request),
      });

      const signupData = await signupResponse.json();

      if (!signupResponse.ok) {
        alert(signupData.message || "Error al registrarse");
        return;
      }

    
      const loginRequest = {
        username: request.username,
        password: request.password,
      };

      const signinResponse = await fetch("/api/v1/auth/signin", {
        headers: { "Content-Type": "application/json" },
        method: "POST",
        body: JSON.stringify(loginRequest),
      });

      const signinData = await signinResponse.json();

      if (!signinResponse.ok) {
        alert(signinData.message || "Error al iniciar sesión");
        return;
      }

      tokenService.setUser(signinData);
      tokenService.updateLocalAccessToken(signinData.token);
      window.location.href = "/";

    } catch (error) {
      console.error(error);
      alert("Ocurrió un error en la conexión con el servidor");
    }
  }

  return (
    <div className="auth-page-container">
      <h1>Register</h1>
      <div className="auth-form-container login-card">
        <FormGenerator
          ref={registerFormRef}
          inputs={registerFormPlayerInputs}
          onSubmit={handleSubmit}
          numberOfColumns={1}
          listenEnterKey
          buttonText="Save"
          buttonClassName="auth-button"
        />
      </div>
    </div>
  );
}

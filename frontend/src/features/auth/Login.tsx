import React, { useState } from "react";
import { useAuth } from "../../app/providers";

export const Login: React.FC = () => {
  const { login, error, clearError } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();
    setValidationError(null);

    if (!email.trim() || !password.trim()) {
      setValidationError("Vui lòng điền đầy đủ email và mật khẩu");
      return;
    }

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      setValidationError("Định dạng email không đúng");
      return;
    }

    setIsSubmitting(true);
    try {
      await login(email.trim(), password);
    } catch {
      // Error is stored in AuthContext and handled visually below
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1 className="login-logo">OmniCare.</h1>
          <p className="login-subtitle">
            Viettel Omnichannel Customer Care System
          </p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          {validationError && (
            <div className="error-banner" role="alert">
              <span>{validationError}</span>
            </div>
          )}

          {error && (
            <div className="error-banner" role="alert">
              <span>{error}</span>
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="email-input">
              Email
            </label>
            <input
              id="email-input"
              type="text"
              className="form-input"
              placeholder="nhanvien@example.test"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setValidationError(null);
              }}
              disabled={isSubmitting}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password-input">
              Mật khẩu
            </label>
            <input
              id="password-input"
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setValidationError(null);
              }}
              disabled={isSubmitting}
            />
          </div>

          <button
            type="submit"
            className="login-submit-btn"
            disabled={isSubmitting}
          >
            {isSubmitting && (
              <div className="inline-loader" data-testid="spinner"></div>
            )}
            <span>Đăng nhập</span>
          </button>
        </form>
      </div>
    </div>
  );
};

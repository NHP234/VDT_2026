import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { Login } from "../features/auth/Login";
import { useAuth } from "../app/providers";

// Mock the useAuth hook
vi.mock("../app/providers", () => ({
  useAuth: vi.fn(),
}));

describe("Login Component", () => {
  const mockLogin = vi.fn();
  const mockClearError = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    (useAuth as any).mockReturnValue({
      login: mockLogin,
      error: null,
      clearError: mockClearError,
    });
  });

  it("renders login form inputs and submit button", () => {
    render(<Login />);
    expect(screen.getByLabelText(/Email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Mật khẩu/i)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Đăng nhập/i }),
    ).toBeInTheDocument();
  });

  it("shows error if fields are empty", async () => {
    render(<Login />);
    const submitBtn = screen.getByRole("button", { name: /Đăng nhập/i });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/Vui lòng điền đầy đủ email và mật khẩu/i),
    ).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it("shows error if email format is invalid", async () => {
    render(<Login />);
    const emailInput = screen.getByLabelText(/Email/i);
    const passwordInput = screen.getByLabelText(/Mật khẩu/i);
    const submitBtn = screen.getByRole("button", { name: /Đăng nhập/i });

    fireEvent.change(emailInput, { target: { value: "invalid-email" } });
    fireEvent.change(passwordInput, { target: { value: "password123" } });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/Định dạng email không đúng/i),
    ).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it("triggers login on valid inputs submit", async () => {
    render(<Login />);
    const emailInput = screen.getByLabelText(/Email/i);
    const passwordInput = screen.getByLabelText(/Mật khẩu/i);
    const submitBtn = screen.getByRole("button", { name: /Đăng nhập/i });

    fireEvent.change(emailInput, { target: { value: "agent@example.test" } });
    fireEvent.change(passwordInput, {
      target: { value: "change-me-local-only" },
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith(
        "agent@example.test",
        "change-me-local-only",
      );
    });
  });

  it("shows loading indicator when submitting", () => {
    (useAuth as any).mockReturnValue({
      login: mockLogin,
      error: null,
      clearError: mockClearError,
    });

    render(<Login />);

    // Simulate active form submit that sets state to loading
    // In this mock, we can render the loading state directly by having useAuth return isSubmitting/loading flags or we mock the button behavior.
    // Wait, the Login component has isSubmitting controlled locally by useState, but when login() is pending it waits. Let's mock a pending promise for login.
    mockLogin.mockImplementation(() => new Promise(() => {})); // never resolves to hold loading state

    const emailInput = screen.getByLabelText(/Email/i);
    const passwordInput = screen.getByLabelText(/Mật khẩu/i);
    const submitBtn = screen.getByRole("button", { name: /Đăng nhập/i });

    fireEvent.change(emailInput, { target: { value: "agent@example.test" } });
    fireEvent.change(passwordInput, {
      target: { value: "change-me-local-only" },
    });
    fireEvent.click(submitBtn);

    expect(screen.getByTestId("spinner")).toBeInTheDocument();
    expect(submitBtn).toBeDisabled();
  });

  it("renders generic authentication error from context", () => {
    (useAuth as any).mockReturnValue({
      login: mockLogin,
      error: "Thông tin đăng nhập không hợp lệ",
      clearError: mockClearError,
    });

    render(<Login />);
    expect(
      screen.getByText("Thông tin đăng nhập không hợp lệ"),
    ).toBeInTheDocument();
  });
});

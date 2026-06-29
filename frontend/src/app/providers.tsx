import React, { createContext, useContext, useState, useEffect } from "react";
import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { Agent } from "../types";
import { authApi } from "../api/authApi";
import { HttpError } from "../api/httpClient";

interface AuthContextType {
  isAuthenticated: boolean;
  agent: Agent | null;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: false,
    },
  },
});

export const AuthProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [agent, setAgent] = useState<Agent | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const clearSession = () => {
    localStorage.removeItem("omnicare_token");
    localStorage.removeItem("omnicare_agent");
    setAgent(null);
    setIsAuthenticated(false);
  };

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem("omnicare_token");
      const savedAgent = localStorage.getItem("omnicare_agent");

      if (token && savedAgent) {
        try {
          // Verify with server
          const currentAgent = await authApi.getMe();
          setAgent(currentAgent);
          setIsAuthenticated(true);
          localStorage.setItem("omnicare_agent", JSON.stringify(currentAgent));
        } catch {
          clearSession();
        }
      }
      setIsLoading(false);
    };

    initAuth();

    // Listen to 401 events from httpClient
    const handleUnauthorized = () => {
      clearSession();
    };

    window.addEventListener("omnicare_unauthorized", handleUnauthorized);
    return () => {
      window.removeEventListener("omnicare_unauthorized", handleUnauthorized);
    };
  }, []);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authApi.login(email, password);
      localStorage.setItem("omnicare_token", response.accessToken);
      localStorage.setItem("omnicare_agent", JSON.stringify(response.agent));
      setAgent(response.agent);
      setIsAuthenticated(true);
    } catch (err) {
      if (err instanceof HttpError) {
        setError(
          err.problem.detail ||
            err.problem.title ||
            "Thông tin đăng nhập không hợp lệ",
        );
      } else {
        setError("Không thể kết nối đến máy chủ xác thực");
      }
      clearSession();
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await authApi.logout();
    } catch {
      // Ignored for client session logout
    } finally {
      clearSession();
      setIsLoading(false);
    }
  };

  const clearError = () => setError(null);

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        agent,
        isLoading,
        error,
        login,
        logout,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export const AppProviders: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
};

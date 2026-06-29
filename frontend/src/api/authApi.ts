import { httpClient } from "./httpClient";
import type { Agent, LoginResponse } from "../types";

export const authApi = {
  login: async (email: string, password: string): Promise<LoginResponse> => {
    return httpClient<LoginResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
  },

  logout: async (): Promise<void> => {
    return httpClient<void>("/api/v1/auth/logout", {
      method: "POST",
    });
  },

  getMe: async (): Promise<Agent> => {
    return httpClient<Agent>("/api/v1/auth/me", {
      method: "GET",
    });
  },
};

import { httpClient } from "./httpClient";
import type { Agent } from "../types";

export const agentsApi = {
  list: async (): Promise<Agent[]> => {
    return httpClient<Agent[]>("/api/v1/agents", {
      method: "GET",
    });
  },
};

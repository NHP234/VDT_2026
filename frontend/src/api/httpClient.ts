import type { ProblemDetails } from "../types";

const getBaseUrl = (): string => {
  return (
    (import.meta.env.VITE_API_BASE_URL as string) || "http://localhost:8080"
  );
};

export class HttpError extends Error {
  public problem: ProblemDetails;
  constructor(problem: ProblemDetails) {
    super(problem.detail || problem.title || "HTTP Request Failed");
    this.problem = problem;
    this.name = "HttpError";
  }
}

interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
}

export async function httpClient<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const baseUrl = getBaseUrl();
  const token = localStorage.getItem("omnicare_token");

  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let url = `${baseUrl}${path}`;
  if (options.params) {
    const searchParams = new URLSearchParams();
    Object.entries(options.params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        searchParams.append(key, String(value));
      }
    });
    const query = searchParams.toString();
    if (query) {
      url += `?${query}`;
    }
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return {} as T;
  }

  let data: any;
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem("omnicare_token");
      localStorage.removeItem("omnicare_agent");
      // Dispatch custom event to trigger redirect in auth provider
      window.dispatchEvent(new Event("omnicare_unauthorized"));
    }

    const problem: ProblemDetails =
      typeof data === "object"
        ? data
        : {
            title: response.statusText || "Unknown Error",
            status: response.status,
            detail: typeof data === "string" ? data : undefined,
          };
    throw new HttpError(problem);
  }

  return data as T;
}

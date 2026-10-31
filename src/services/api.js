import axios from "axios";
import { readSession, writeSession } from "@/components/login/session";

export const API_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  process.env.REACT_APP_API_URL ||
  process.env.VITE_API_URL ||
  "http://localhost:8080";

export const api = axios.create({ baseURL: API_URL });

function remembered() {
  return Boolean(window.localStorage.getItem("vikingfc.accessToken"));
}

api.interceptors.request.use((config) => {
  if (typeof window === "undefined") return config;
  const session = readSession();
  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`;
  }
  return config;
});

let refreshTask = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config || {};
    const status = error.response?.status;
    const path = String(original.url || "");
    const session = typeof window === "undefined" ? null : readSession();
    const authForm = ["/api/auth/login", "/api/auth/change-password", "/api/auth/refresh"].some((item) => path.includes(item));
    if (status === 403 && error.response?.data?.code === "PASSWORD_CHANGE_REQUIRED" && typeof window !== "undefined") {
      if (!window.location.pathname.startsWith("/change-password")) {
        window.location.assign("/change-password");
      }
    }
    if (status === 401 && !original._retry && session?.refreshToken && !authForm) {
      original._retry = true;
      refreshTask = refreshTask || axios
        .post(`${API_URL}/api/auth/refresh`, { refreshToken: session.refreshToken })
        .then((response) => {
          const data = response.data?.data;
          if (!data?.accessToken) throw new Error(response.data?.message || "Session expired");
          writeSession(
            {
              accessToken: data.accessToken,
              refreshToken: data.refreshToken || session.refreshToken,
              user: {
                ...(session.user || {}),
                email: data.email || session.user?.email,
                fullName: data.fullName || session.user?.fullName,
                role: data.role || session.user?.role,
              },
            },
            remembered(),
          );
          return data.accessToken;
        })
        .finally(() => {
          refreshTask = null;
        });
      const token = await refreshTask;
      original.headers = { ...(original.headers || {}), Authorization: `Bearer ${token}` };
      return api(original);
    }
    const message = error.response?.data?.message || error.message || "Request failed";
    const failure = new Error(message);
    failure.code = error.response?.data?.code || "";
    failure.status = status;
    failure.data = error.response?.data?.data || null;
    return Promise.reject(failure);
  },
);

export async function getData(path, params) {
  const response = await api.get(path, { params });
  if (!response.data || response.data.success === false) {
    throw new Error(response.data?.message || "Request failed");
  }
  return response.data.data;
}

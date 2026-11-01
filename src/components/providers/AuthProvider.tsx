"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { readSession } from "@/components/login/session";
import { api, getData } from "@/services/api";

export type ClubUser = {
  fullName: string;
  email: string;
  role: string;
  phone: string;
  photo: string | null;
  mustChangePassword?: boolean;
};

type AuthValue = {
  user: ClubUser | null;
  ready: boolean;
  setUser: (next: ClubUser) => void;
  setPhoto: (photo: string | null) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthValue | null>(null);

function clearSession() {
  for (const store of [localStorage, sessionStorage]) {
    store.removeItem("vfc_profile");
    store.removeItem("vikingfc.accessToken");
    store.removeItem("vikingfc.refreshToken");
    store.removeItem("vikingfc.user");
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<ClubUser | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const load = () => {
      const session = readSession();
      if (!session?.accessToken) {
        if (!cancelled) {
          setUserState(null);
          setReady(true);
        }
        return;
      }
      getData("/api/auth/me")
        .then((me) => {
          if (cancelled) return;
          setUserState({
            fullName: me.fullName || "",
            email: me.email || "",
            role: me.role || "",
            phone: me.phone || "",
            photo: me.photoUrl || null,
            mustChangePassword: Boolean(me.mustChangePassword),
          });
        })
        .catch(() => {
          if (!cancelled) setUserState(null);
        })
        .finally(() => {
          if (!cancelled) setReady(true);
        });
    };
    load();
    window.addEventListener("vikingfc-session", load);
    return () => {
      cancelled = true;
      window.removeEventListener("vikingfc-session", load);
    };
  }, []);

  const setUser = (next: ClubUser) => setUserState(next);
  const setPhoto = (photo: string | null) => {
    if (user) setUserState({ ...user, photo });
  };
  const logout = () => {
    api.post("/api/session/end", { reason: "LOGOUT" }).catch(() => {});
    clearSession();
    setUserState(null);
  };

  const value = useMemo(() => ({ user, ready, setUser, setPhoto, logout }), [user, ready]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

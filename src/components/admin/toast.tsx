"use client";

import { createContext, useContext } from "react";

export const ToastContext = createContext<(message: string) => void>(() => {});

export function useAdminToast() {
  return useContext(ToastContext);
}

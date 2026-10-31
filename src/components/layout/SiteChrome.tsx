"use client";

import { usePathname } from "next/navigation";
import { Navbar } from "@/components/layout/Navbar";
import { Footer } from "@/components/layout/Footer";
import { Ticker } from "@/components/ui/Ticker";

/** Next.js equivalent of App.jsx: hide the site header and footer on auth routes. */
export const HIDE_LAYOUT_ON = ["/login", "/signup", "/forgot-password", "/change-password", "/verify-otp", "/convocation"];

export function SiteChrome({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() || "/";
  const hide =
    pathname === "/admin" ||
    pathname.startsWith("/admin/") ||
    HIDE_LAYOUT_ON.some((path) => pathname === path || pathname.startsWith(`${path}/`));

  if (hide) return children;

  return (
    <>
      <Navbar />
      <main className="min-h-screen">{children}</main>
      <Ticker />
      <Footer />
    </>
  );
}

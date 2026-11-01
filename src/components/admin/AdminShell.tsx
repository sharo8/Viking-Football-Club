"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import {
  ArrowUp,
  Bell,
  Calendar,
  ChevronDown,
  ChevronRight,
  ClipboardList,
  FileText,
  Handshake,
  Images,
  LayoutDashboard,
  LogOut,
  Mail,
  Menu,
  Moon,
  PanelLeft,
  Search,
  Settings,
  Shield,
  Stethoscope,
  Sun,
  Table2,
  UserCog,
  Users,
  Video,
  Wallet,
} from "lucide-react";
import { club } from "@/lib/data";
import { getData } from "@/services/api";
import { readSession } from "@/components/login/session";
import { useLocale } from "@/components/providers/LocaleProvider";
import { useTheme } from "@/components/providers/ThemeProvider";
import { useAuth } from "@/components/providers/AuthProvider";
import { canSee, isExecutive, ROLE_LABEL, type AdminSection } from "@/lib/admin/access";
import { adminCopy, type AdminLocale } from "@/lib/admin/copy";
import { Avatar } from "@/ui/kit";
import { ToastContext } from "./toast";
import { ChatDock } from "./ChatDock";
import { AttentionBar } from "./AttentionBar";
import "./admin.css";
import { SessionPulse } from "./SessionPulse";
import "@/ui/ui.css";

const NAV: { id: AdminSection; href: string; icon: typeof LayoutDashboard }[] = [
  { id: "dashboard", href: "/admin", icon: LayoutDashboard },
  { id: "players", href: "/admin/players", icon: Users },
  { id: "formerPlayers", href: "/admin/former-players", icon: Users },
  { id: "captains", href: "/admin/captains", icon: Shield },
  { id: "staff", href: "/admin/staff", icon: UserCog },
  { id: "matches", href: "/admin/matches", icon: Calendar },
  { id: "calendar", href: "/admin/calendar", icon: Calendar },
  { id: "lineup", href: "/admin/lineup", icon: ClipboardList },
  { id: "attendance", href: "/admin/attendance", icon: Table2 },
  { id: "finance", href: "/admin/finance", icon: Wallet },
  { id: "bonuses", href: "/admin/bonuses", icon: Wallet },
  { id: "fines", href: "/admin/fines", icon: FileText },
  { id: "medical", href: "/admin/medical", icon: Stethoscope },
  { id: "news", href: "/admin/news", icon: FileText },
  { id: "gallery", href: "/admin/gallery", icon: Images },
  { id: "videos", href: "/admin/videos", icon: Video },
  { id: "sponsors", href: "/admin/sponsors", icon: Handshake },
  { id: "partners", href: "/admin/partners", icon: Handshake },
  { id: "transfers", href: "/admin/transfers", icon: Users },
  { id: "marketing", href: "/admin/marketing", icon: Mail },
  { id: "table", href: "/admin/table", icon: Table2 },
  { id: "messages", href: "/admin/messages", icon: Mail },
  { id: "emails", href: "/admin/emails", icon: Mail },
  { id: "notifications", href: "/admin/notifications", icon: Bell },
  { id: "users", href: "/admin/administration", icon: Shield },
  { id: "roles", href: "/admin/roles", icon: Shield },
  { id: "approvals", href: "/admin/approvals", icon: Shield },
  { id: "reports", href: "/admin/reports", icon: FileText },
  { id: "audit", href: "/admin/audit", icon: Shield },
  { id: "settings", href: "/admin/settings", icon: Settings },
];


const GROUPS: { id: string; label: string; ids: AdminSection[] }[] = [
  { id: "club", label: "groupClub", ids: ["dashboard", "players", "formerPlayers", "captains", "staff"] },
  { id: "sport", label: "groupSport", ids: ["matches", "calendar", "lineup", "table", "attendance", "medical"] },
  { id: "finance", label: "groupFinance", ids: ["finance", "bonuses", "fines"] },
  { id: "communication", label: "groupCommunication", ids: ["marketing", "messages", "notifications", "emails", "news", "gallery", "videos"] },
  { id: "administration", label: "groupAdministration", ids: ["users", "roles", "approvals", "reports", "audit", "settings", "partners", "sponsors", "transfers"] },
];

function sectionFromPath(pathname: string): AdminSection {
  const part = pathname.split("/")[2];
  if (part === "administration" || part === "users") return "users";
  if (part === "former-players") return "formerPlayers";
  return (part || "dashboard") as AdminSection;
}

export function AdminShell({ children }: { children: ReactNode }) {
  const pathname = usePathname() || "/admin";
  const router = useRouter();
  const reduced = useReducedMotion();
  const { locale, setLocale } = useLocale();
  const { theme, toggleTheme } = useTheme();
  const { user, ready, logout } = useAuth();
  const copy = adminCopy[(locale as AdminLocale) in adminCopy ? (locale as AdminLocale) : "en"];
  const mainRef = useRef<HTMLElement>(null);
  const [collapsed, setCollapsed] = useState(false);
  const [drawer, setDrawer] = useState(false);
  const [langOpen, setLangOpen] = useState(false);
  const [notesOpen, setNotesOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [palette, setPalette] = useState(false);
  const [query, setQuery] = useState("");
  const [activeHit, setActiveHit] = useState(0);
  const [read, setRead] = useState<string[]>([]);
  const [notes, setNotes] = useState<{ id: string; text: string; read?: boolean }[]>([]);
  const [showTop, setShowTop] = useState(false);
  const [toast, setToast] = useState("");
  const [closed, setClosed] = useState<Record<string, boolean>>({});
  const [pendingApprovals, setPendingApprovals] = useState(0);

  useEffect(() => {
    setCollapsed(localStorage.getItem("vfc_sidebar") === "collapsed");
    setRead(JSON.parse(localStorage.getItem("vfc_notes_read") || "[]"));
    try {
      setClosed(JSON.parse(localStorage.getItem("vfc_nav_groups") || "{}"));
    } catch {
      setClosed({});
    }
  }, []);

  useEffect(() => {
    if (!ready) return;
    if (user?.mustChangePassword) router.replace("/change-password");
    else if (!user && !readSession()?.accessToken) router.replace("/login");
  }, [ready, user, router]);

  useEffect(() => {
    if (!user || !isExecutive(user.role)) return undefined;
    const pull = () => getData("/api/deletion-requests", { status: "PENDING" }).then((rows) => setPendingApprovals((rows || []).length)).catch(() => setPendingApprovals(0));
    pull();
    const timer = window.setInterval(pull, 15000);
    return () => window.clearInterval(timer);
  }, [user]);

  useEffect(() => {
    if (!user || user.mustChangePassword) return;
    getData("/api/notifications")
      .then((rows) => setNotes((rows || []).map((row: { id?: string | number; title?: string; body?: string; read?: boolean }) => ({ id: String(row.id), text: row.title || row.body || "", read: Boolean(row.read) }))))
      .catch(() => setNotes([]));
  }, [user]);

  useEffect(() => {
    setDrawer(false);
    setLangOpen(false);
    setNotesOpen(false);
    setProfileOpen(false);
    setPalette(false);
    mainRef.current?.scrollTo({ top: 0, behavior: reduced ? "auto" : "smooth" });
  }, [pathname, reduced]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setPalette(true);
      }
      if (event.key === "Escape") setPalette(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const notify = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 2200);
  };

  const visibleNav = NAV.filter((item) => user && canSee(user.role, item.id));
  const current = sectionFromPath(pathname);
  const hits = useMemo(() => {
    const q = query.trim().toLowerCase();
    const pages = visibleNav.map((item) => ({
      label: copy[item.id],
      href: item.href,
      kind: "Page",
    }));
    return pages.filter((item) => !q || item.label.toLowerCase().includes(q)).slice(0, 8);
  }, [query, visibleNav, copy]);

  const unread = notes.filter((note) => !note.read && !read.includes(note.id)).length;

  if (!user || user.mustChangePassword) return null;

  return (
    <ToastContext.Provider value={notify}>
      <div className="admin-shell" data-collapsed={collapsed ? "true" : "false"} data-drawer={drawer ? "open" : "closed"}>
        <button type="button" className="side-backdrop" aria-label={copy.close} onClick={() => setDrawer(false)} />
        <aside className="admin-side">
          <Link href="/admin" className="admin-brand">
            <img src={club.logo} alt="" />
            <div>
              VIKING
              <small>{copy.clubOffice}</small>
            </div>
          </Link>
          <nav className="admin-nav" aria-label="Admin">
            {GROUPS.map((group) => {
              const items = visibleNav.filter((item) => group.ids.includes(item.id));
              if (!items.length) return null;
              const shut = Boolean(closed[group.id]);
              return (
                <div key={group.id} className="nav-group">
                  <button
                    type="button"
                    className="nav-group-label"
                    aria-expanded={!shut}
                    onClick={() => {
                      const next = { ...closed, [group.id]: !shut };
                      setClosed(next);
                      localStorage.setItem("vfc_nav_groups", JSON.stringify(next));
                    }}
                  >
                    <span>{(copy as Record<string, string>)[group.label] || group.label}</span>
                    <ChevronDown size={14} className={shut ? "is-shut" : ""} />
                  </button>
                  {shut ? null : items.map((item) => {
                    const active = item.href === "/admin" ? pathname === "/admin" : pathname.startsWith(item.href);
                    const Icon = item.icon;
                    return (
                      <Link key={item.id} href={item.href} className={`admin-link${active ? " is-active" : ""}`} title={copy[item.id]}>
                        {active ? <motion.span layoutId="admin-pill" className="admin-pill" /> : null}
                        <Icon size={18} />
                        <span>{copy[item.id]}</span>
                        {item.id === "approvals" && pendingApprovals > 0 ? <span className="badge-dot">{pendingApprovals}</span> : null}
                      </Link>
                    );
                  })}
                </div>
              );
            })}
          </nav>
          <div className="admin-profile">
            <Avatar name={user.fullName} src={user.photo || undefined} size="sm" ring />
            <p>
              {user.fullName}
              <br />
              <small>{ROLE_LABEL[user.role] || user.role}</small>
            </p>
          </div>
          <button
            type="button"
            className="admin-logout"
            onClick={() => {
              logout();
              router.push("/login");
            }}
          >
            <LogOut size={16} />
            <span>{copy.logout}</span>
          </button>
        </aside>

        <div className="admin-stage">
        <header className="admin-head">
          <button type="button" className="icon-btn mobile-only" aria-label={copy.menu} onClick={() => setDrawer(true)}>
            <Menu size={18} />
          </button>
          <button
            type="button"
            className="icon-btn desktop-only"
            aria-label={collapsed ? copy.expand : copy.collapse}
            onClick={() => {
              const next = !collapsed;
              setCollapsed(next);
              localStorage.setItem("vfc_sidebar", next ? "collapsed" : "open");
            }}
          >
            <PanelLeft size={18} />
          </button>
          <div className="admin-crumbs">
            {current === "dashboard" ? (
              <strong>{copy.dashboard}</strong>
            ) : (
              <Link href="/admin">{copy.dashboard}</Link>
            )}
            {current !== "dashboard" ? (
              <>
                <ChevronRight size={14} />
                <strong>{copy[current] || current}</strong>
              </>
            ) : null}
          </div>
          <div className="admin-head-actions">
            <button type="button" className="icon-btn" aria-label={copy.search} onClick={() => setPalette(true)}>
              <Search size={18} />
            </button>
            <div style={{ position: "relative" }}>
              <button type="button" className="icon-btn" aria-label={copy.language} onClick={() => setLangOpen((v) => !v)}>
                {locale.toUpperCase()}
              </button>
              <AnimatePresence>
                {langOpen ? (
                  <motion.div className="admin-pop" initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }}>
                    {(["en", "fr", "rw"] as const).map((code) => (
                      <button
                        key={code}
                        type="button"
                        onClick={() => {
                          setLocale(code);
                          setLangOpen(false);
                        }}
                        style={{ fontWeight: locale === code ? 700 : 500 }}
                      >
                        {code.toUpperCase()}
                      </button>
                    ))}
                  </motion.div>
                ) : null}
              </AnimatePresence>
            </div>
            <button type="button" className="icon-btn" aria-label={copy.theme} onClick={toggleTheme}>
              <AnimatePresence mode="wait" initial={false}>
                <motion.span key={theme} initial={{ rotate: -80, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: 80, opacity: 0 }} style={{ display: "grid" }}>
                  {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
                </motion.span>
              </AnimatePresence>
            </button>
            <ChatDock />
            <div style={{ position: "relative" }}>
              <button type="button" className="icon-btn" aria-label={copy.notifications} onClick={() => setNotesOpen((v) => !v)}>
                <Bell size={18} />
                {notes.filter((note) => !note.read && !read.includes(note.id)).length > 0 || pendingApprovals > 0 ? <span className="badge-dot">{Math.max(notes.filter((note) => !note.read && !read.includes(note.id)).length, pendingApprovals)}</span> : null}
              </button>
              <AnimatePresence>
                {notesOpen ? (
                  <motion.div className="admin-pop" initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                    {notes.length ? notes.map((note) => (
                      <p key={note.id} style={{ margin: "8px 10px", fontSize: 13, opacity: note.read || read.includes(note.id) ? 0.55 : 1 }}>
                        {note.text}
                      </p>
                    )) : <p style={{ margin: "8px 10px", fontSize: 13 }}>No notifications</p>}
                    <button
                      type="button"
                      onClick={() => {
                        const ids = notes.map((note) => note.id);
                        setRead(ids);
                      }}
                    >
                      {copy.markRead}
                    </button>
                  </motion.div>
                ) : null}
              </AnimatePresence>
            </div>
            <div style={{ position: "relative" }}>
              <button type="button" className="admin-user" aria-label={user.fullName} onClick={() => setProfileOpen((v) => !v)}>
                <Avatar name={user.fullName} src={user.photo || undefined} size="md" ring />
                <span>
                  <strong>{user.fullName}</strong>
                  <small>{ROLE_LABEL[user.role] || user.role}</small>
                </span>
                <ChevronDown size={16} aria-hidden="true" />
              </button>
              <AnimatePresence>
                {profileOpen ? (
                  <motion.div className="admin-pop" initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                    <p style={{ margin: "8px 10px 2px", fontWeight: 700 }}>{user.fullName}</p>
                    <p style={{ margin: "0 10px 8px", color: "var(--accent-brand)", fontSize: 12 }}>{ROLE_LABEL[user.role] || user.role}</p>
                    <Link href="/admin/profile" onClick={() => setProfileOpen(false)}>{copy.myProfile}</Link>
                    <Link href="/admin/profile#photo" onClick={() => setProfileOpen(false)}>{copy.changePhoto}</Link>
                    <Link href="/admin/profile#password" onClick={() => setProfileOpen(false)}>{copy.changePassword}</Link>
                    <Link href="/admin/settings" onClick={() => setProfileOpen(false)}>{copy.settings}</Link>
                    <button
                      type="button"
                      onClick={() => {
                        logout();
                        router.push("/login");
                      }}
                    >
                      {copy.logout}
                    </button>
                  </motion.div>
                ) : null}
              </AnimatePresence>
            </div>
          </div>
        </header>

        <AttentionBar />

        <main
          ref={mainRef}
          className="admin-main"
          onScroll={(event) => setShowTop(event.currentTarget.scrollTop > 400)}
        >
          <SessionPulse />
          {children}
        </main>
        </div>

        {showTop ? (
          <button type="button" className="top-btn" aria-label={copy.backToTop} onClick={() => mainRef.current?.scrollTo({ top: 0, behavior: "smooth" })}>
            <ArrowUp size={18} />
          </button>
        ) : null}

        <AnimatePresence>
          {palette ? (
            <motion.div className="drawer-back" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setPalette(false)}>
              <motion.div
                className="admin-card"
                style={{ width: "min(560px, calc(100% - 32px))", margin: "12vh auto", padding: 12 }}
                initial={{ opacity: 0, scale: 0.98, y: 10 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.98 }}
                onClick={(event) => event.stopPropagation()}
                role="dialog"
                aria-label={copy.command}
              >
                <input
                  className="admin-search"
                  autoFocus
                  value={query}
                  placeholder={copy.command}
                  onChange={(event) => {
                    setQuery(event.target.value);
                    setActiveHit(0);
                  }}
                  onKeyDown={(event) => {
                    if (event.key === "ArrowDown") setActiveHit((i) => Math.min(hits.length - 1, i + 1));
                    if (event.key === "ArrowUp") setActiveHit((i) => Math.max(0, i - 1));
                    if (event.key === "Enter" && hits[activeHit]) {
                      router.push(hits[activeHit].href);
                      setPalette(false);
                      setQuery("");
                    }
                  }}
                />
                <div style={{ marginTop: 8 }}>
                  {hits.length === 0 ? <p style={{ padding: 10, color: "var(--text-muted)" }}>{copy.noResults}</p> : null}
                  {hits.map((hit, index) => (
                    <button
                      key={`${hit.kind}-${hit.label}`}
                      type="button"
                      onClick={() => {
                        router.push(hit.href);
                        setPalette(false);
                        setQuery("");
                      }}
                      style={{
                        display: "flex",
                        width: "100%",
                        justifyContent: "space-between",
                        padding: "10px 12px",
                        border: 0,
                        borderRadius: 8,
                        background: index === activeHit ? "var(--surface-2)" : "transparent",
                        color: "inherit",
                        font: "inherit",
                        cursor: "pointer",
                      }}
                    >
                      <span>{hit.label}</span>
                      <small style={{ color: "var(--text-muted)" }}>{hit.kind}</small>
                    </button>
                  ))}
                </div>
              </motion.div>
            </motion.div>
          ) : null}
        </AnimatePresence>
        <AnimatePresence>
          {toast ? (
            <motion.div className="toast" role="status" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
              {toast}
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

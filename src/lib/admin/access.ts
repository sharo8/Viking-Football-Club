export const EXECUTIVE_ROLES = ["PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER"] as const;

export type AdminSection =
  | "dashboard"
  | "players"
  | "captains"
  | "staff"
  | "matches"
  | "news"
  | "gallery"
  | "videos"
  | "sponsors"
  | "table"
  | "users"
  | "messages"
  | "emails"
  | "notifications"
  | "settings"
  | "finance"
  | "attendance"
  | "fines"
  | "lineup"
  | "medical"
  | "partners"
  | "transfers"
  | "marketing"
  | "reports"
  | "audit"
  | "profile";

const ALL: AdminSection[] = [
  "dashboard",
  "players",
  "captains",
  "staff",
  "matches",
  "news",
  "gallery",
  "videos",
  "sponsors",
  "table",
  "users",
  "messages",
  "notifications",
  "settings",
  "finance",
  "attendance",
  "fines",
  "lineup",
  "medical",
  "partners",
  "transfers",
  "marketing",
  "reports",
  "audit",
  "profile",
];

const BY_ROLE: Record<string, AdminSection[]> = {
  ASSISTANT_MANAGER: ALL.filter((id) => !["finance", "audit", "users"].includes(id)),
  DOCTOR: ["dashboard", "players", "captains", "medical", "attendance", "lineup", "notifications", "profile"],
  SOCIAL_MEDIA_MANAGER: ["dashboard", "players", "captains", "news", "gallery", "videos", "messages", "marketing", "lineup", "notifications", "profile"],
  MARKETING_MANAGER: ["dashboard", "players", "captains", "news", "gallery", "sponsors", "partners", "marketing", "lineup", "messages", "notifications", "profile"],
  HEAD_COACH: ["dashboard", "players", "captains", "matches", "attendance", "lineup", "medical", "notifications", "profile"],
  ASSISTANT_COACH: ["dashboard", "players", "captains", "matches", "attendance", "lineup", "notifications", "profile"],
  GOALKEEPER_COACH: ["dashboard", "players", "captains", "matches", "lineup", "notifications", "profile"],
};

export function isExecutive(role: string) {
  return (EXECUTIVE_ROLES as readonly string[]).includes(role);
}

const COACH_ROLES = ["HEAD_COACH", "ASSISTANT_COACH", "GOALKEEPER_COACH", "ASSISTANT_MANAGER"];
const MEDIA_ROLES = ["SOCIAL_MEDIA_MANAGER", "MARKETING_MANAGER"];

export function dashScope(role: string) {
  if (isExecutive(role)) {
    return { people: true, sport: true, attendance: true, injuries: true, fines: true, finance: true, partners: true, next: true, analysis: true };
  }
  if (role === "DOCTOR") {
    return { people: true, sport: false, attendance: true, injuries: true, fines: false, finance: false, partners: false, next: true, analysis: false };
  }
  if (MEDIA_ROLES.includes(role)) {
    return { people: false, sport: true, attendance: false, injuries: false, fines: false, finance: false, partners: true, next: true, analysis: false };
  }
  if (COACH_ROLES.includes(role)) {
    return { people: true, sport: true, attendance: true, injuries: true, fines: true, finance: false, partners: false, next: true, analysis: true };
  }
  return { people: true, sport: true, attendance: false, injuries: false, fines: false, finance: false, partners: false, next: true, analysis: false };
}

export function canSee(role: string, section: string) {
  if (isExecutive(role)) return true;
  const allowed = BY_ROLE[role] ?? ["dashboard", "profile"];
  return allowed.includes(section as AdminSection);
}

export const ROLE_LABEL: Record<string, string> = {
  PRESIDENT: "President",
  GENERAL_MANAGER: "General Manager",
  FINANCE_MANAGER: "Finance Manager",
  ASSISTANT_MANAGER: "Assistant Manager",
  DOCTOR: "Doctor",
  SOCIAL_MEDIA_MANAGER: "Social Media",
  MARKETING_MANAGER: "Marketing",
  HEAD_COACH: "Head Coach",
  ASSISTANT_COACH: "Assistant Coach",
  GOALKEEPER_COACH: "Goalkeeper Coach",
};

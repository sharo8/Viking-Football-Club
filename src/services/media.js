import { API_URL } from "@/services/api";

export function getMediaUrl(src) {
  if (!src) return "";
  const value = String(src);
  if (value.startsWith("blob:") || value.startsWith("data:") || value.startsWith("http://") || value.startsWith("https://")) {
    return value;
  }
  const base = API_URL.replace(/\/$/, "");
  return value.startsWith("/") ? `${base}${value}` : `${base}/${value}`;
}

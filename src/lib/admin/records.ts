export type Row = Record<string, string | number>;

const KEY = "vfc_admin_records";

function readAll(): Record<string, Row[]> {
  if (typeof window === "undefined") return {};
  try {
    const parsed = JSON.parse(localStorage.getItem(KEY) || "{}");
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

export function readRows(bucket: string, seed: Row[]): Row[] {
  const all = readAll();
  if (!Array.isArray(all[bucket])) {
    all[bucket] = seed;
    if (typeof window !== "undefined") localStorage.setItem(KEY, JSON.stringify(all));
    return seed;
  }
  return all[bucket];
}

export function writeRows(bucket: string, rows: Row[]) {
  const all = readAll();
  all[bucket] = rows;
  localStorage.setItem(KEY, JSON.stringify(all));
}

export function toCsv(rows: Row[]) {
  if (!rows.length) return "";
  const keys = Object.keys(rows[0]);
  const esc = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`;
  return [keys.join(","), ...rows.map((row) => keys.map((key) => esc(row[key] ?? "")).join(","))].join("\n");
}

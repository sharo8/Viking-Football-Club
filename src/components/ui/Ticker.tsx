"use client";

import { usePathname } from "next/navigation";
import { matches, leagueTable, club } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";

export function Ticker() {
  const pathname = usePathname();
  const { t } = useLocale();

  if (pathname.startsWith("/admin") || pathname.startsWith("/login")) {
    return null;
  }

  const upcoming = matches.find((m) => m.status === "upcoming");
  const played = matches.filter((m) => m.status === "played");
  const viking = leagueTable.find((r) => r.team === "Viking FC");

  const items = [
    upcoming
      ? `${t.hero.nextMatch}: ${upcoming.home ? club.name : upcoming.opponent} ${t.match.vs} ${upcoming.home ? upcoming.opponent : club.name} · ${upcoming.date} ${upcoming.time}`
      : null,
    ...played.slice(0, 3).map(
      (m) =>
        `${t.match.latestResults}: ${m.home ? club.name : m.opponent} ${m.scoreHome}-${m.scoreAway} ${m.home ? m.opponent : club.name}`,
    ),
    viking
      ? `${t.match.leagueTable}: ${club.name} · ${t.match.pos} ${viking.pos} · ${viking.pts} ${t.match.pts}`
      : null,
    `${club.slogan}`,
  ].filter(Boolean) as string[];

  const loop = [...items, ...items];

  return (
    <div className="ticker-wrap relative z-40">
      <div className="ticker-track" aria-hidden="true">
        {loop.map((text, i) => (
          <span key={`${text}-${i}`} className="ticker-item">
            <span className="ticker-dot" />
            <strong>VIKING</strong>
            <span>{text}</span>
          </span>
        ))}
      </div>
    </div>
  );
}

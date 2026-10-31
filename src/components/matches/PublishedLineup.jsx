"use client";

import { useEffect, useState } from "react";
import { getData } from "@/services/api";

export function PublishedLineup() {
  const [sheet, setSheet] = useState(null);

  useEffect(() => {
    getData("/api/public/lineups/next").then((data) => {
      if (data?.published) setSheet(data);
    }).catch(() => setSheet(null));
  }, []);

  if (!sheet) return null;
  const starters = (sheet.slots || []).filter((slot) => slot.selection === "STARTING");
  return (
    <section className="section-pad bg-[var(--bg)]">
      <div className="container-page">
        <p className="section-label">Lineup · {sheet.formation}</p>
        <h2 className="font-display text-3xl">Viking FC vs {sheet.match?.opponent}</h2>
        <ul className="mt-4 grid gap-2 md:grid-cols-2">
          {starters.map((slot) => (
            <li key={slot.playerId}>#{slot.jersey} {slot.name} · {slot.positionSlot}</li>
          ))}
        </ul>
      </div>
    </section>
  );
}

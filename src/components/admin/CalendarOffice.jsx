"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { getData } from "@/services/api";
import { Button } from "@/ui/kit";
import "./matches.css";

function monthRange(anchor) {
  const start = new Date(anchor.getFullYear(), anchor.getMonth(), 1);
  const end = new Date(anchor.getFullYear(), anchor.getMonth() + 1, 1);
  return { start, end };
}

export function CalendarOffice() {
  const [anchor, setAnchor] = useState(() => new Date());
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");
  const [layers, setLayers] = useState("matches,training,birthdays,deadlines");

  useEffect(() => {
    const { start, end } = monthRange(anchor);
    getData("/api/calendar", { from: start.toISOString(), to: end.toISOString(), layers })
      .then(setEvents)
      .catch((err) => setError(err.message));
  }, [anchor, layers]);

  const cells = useMemo(() => {
    const { start } = monthRange(anchor);
    const first = new Date(start);
    first.setDate(1 - ((start.getDay() + 6) % 7));
    return Array.from({ length: 42 }, (_, index) => {
      const day = new Date(first);
      day.setDate(first.getDate() + index);
      return day;
    });
  }, [anchor]);

  return (
    <section className="calendar-page">
      <header className="matches-head">
        <h1>{anchor.toLocaleString("en-GB", { month: "long", year: "numeric", timeZone: "Africa/Kigali" })}</h1>
        <div className="matches-actions">
          <Button onClick={() => setAnchor(new Date())}>Today</Button>
          <Button onClick={() => setAnchor(new Date(anchor.getFullYear(), anchor.getMonth() - 1, 1))}>Previous</Button>
          <Button onClick={() => setAnchor(new Date(anchor.getFullYear(), anchor.getMonth() + 1, 1))}>Next</Button>
          <Link href="/admin/matches"><Button variant="primary">Matches</Button></Link>
          <a href={`http://localhost:8080/api/calendar/ics?from=${monthRange(anchor).start.toISOString()}&to=${monthRange(anchor).end.toISOString()}`}>iCal</a>
        </div>
      </header>
      <label className="tool-switch"><input type="checkbox" checked={layers.includes("training")} onChange={(event) => setLayers(event.target.checked ? "matches,training,birthdays,deadlines,medical" : "matches,deadlines")} /> Training, birthdays and medical</label>
      {error ? <p className="banner">{error}</p> : null}
      <div className="month-grid">
        {cells.map((day) => {
          const key = day.toISOString().slice(0, 10);
          const mine = events.filter((item) => String(item.at).slice(0, 10) === key);
          const today = key === new Date().toISOString().slice(0, 10);
          return (
            <div key={key} className={today ? "today" : ""}>
              <strong>{day.getDate()}</strong>
              {mine.map((item) => (
                <Link key={`${item.kind}-${item.id}`} href={item.kind === "MATCH" ? `/admin/lineup?matchId=${item.id}` : "/admin/matches"}>{item.title}</Link>
              ))}
            </div>
          );
        })}
      </div>
    </section>
  );
}

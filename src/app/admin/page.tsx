"use client";

import Link from "next/link";
import {
  LayoutDashboard,
  Users,
  UserCog,
  Calendar,
  Newspaper,
  Images,
  Video,
  Handshake,
  Table2,
  Shield,
  Mail,
  Settings,
  BarChart3,
  FileText,
  LogOut,
} from "lucide-react";
import { players, staff, matches, news, leagueTable } from "@/lib/data";

const nav = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin#players", label: "Players", icon: Users },
  { href: "/admin#staff", label: "Staff", icon: UserCog },
  { href: "/admin#matches", label: "Matches", icon: Calendar },
  { href: "/admin#news", label: "News", icon: Newspaper },
  { href: "/admin#gallery", label: "Gallery", icon: Images },
  { href: "/admin#videos", label: "Videos", icon: Video },
  { href: "/admin#sponsors", label: "Sponsors", icon: Handshake },
  { href: "/admin#table", label: "League Table", icon: Table2 },
  { href: "/admin#users", label: "Users", icon: Shield },
  { href: "/admin#messages", label: "Messages", icon: Mail },
  { href: "/admin#settings", label: "Settings", icon: Settings },
  { href: "/admin#reports", label: "Reports", icon: FileText },
  { href: "/admin#analytics", label: "Analytics", icon: BarChart3 },
];

export default function AdminPage() {
  const cards = [
    { label: "Players", value: players.length },
    { label: "Staff", value: staff.length },
    { label: "Matches", value: matches.length },
    { label: "News", value: news.length },
    { label: "League Teams", value: leagueTable.length },
    { label: "Messages", value: 12 },
  ];

  return (
    <div className="min-h-screen bg-[#07111f] text-white">
      <div className="flex min-h-screen">
        <aside className="hidden w-64 shrink-0 border-r border-white/10 bg-navy-deep p-4 lg:block">
          <Link href="/" className="mb-8 block font-display text-2xl tracking-wider">
            VIKING <span className="text-red">ADMIN</span>
          </Link>
          <nav className="space-y-1">
            {nav.map((item) => (
              <a
                key={item.label}
                href={item.href}
                className="flex items-center gap-3 px-3 py-2.5 text-sm text-white/70 transition hover:bg-white/5 hover:text-white"
              >
                <item.icon size={16} />
                {item.label}
              </a>
            ))}
          </nav>
          <Link
            href="/"
            className="mt-8 flex items-center gap-3 px-3 py-2.5 text-sm text-red"
          >
            <LogOut size={16} />
            Back to Site
          </Link>
        </aside>

        <main className="flex-1 p-6 md:p-8">
          <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
            <div>
              <h1 className="font-display text-4xl tracking-wider">Dashboard</h1>
              <p className="text-sm text-white/55">
                Secure admin overview — demo UI ready for Spring Boot API
              </p>
            </div>
            <Link href="/" className="btn-outline border-white/30 text-white lg:hidden">
              Back to Site
            </Link>
          </div>

          <div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {cards.map((card) => (
              <div
                key={card.label}
                className="border border-white/10 bg-navy px-5 py-6"
              >
                <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-white/50">
                  {card.label}
                </p>
                <p className="mt-2 font-display text-5xl text-red">{card.value}</p>
              </div>
            ))}
          </div>

          <div id="players" className="mb-8 border border-white/10 bg-navy">
            <div className="border-b border-white/10 px-5 py-4 font-display text-2xl tracking-wide">
              Players
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="text-left text-[11px] uppercase tracking-wider text-white/50">
                  <tr>
                    <th className="px-5 py-3">#</th>
                    <th className="px-5 py-3">Name</th>
                    <th className="px-5 py-3">Position</th>
                    <th className="px-5 py-3">Age</th>
                  </tr>
                </thead>
                <tbody>
                  {players.map((p) => (
                    <tr key={p.id} className="border-t border-white/10">
                      <td className="px-5 py-3 text-red">{p.number}</td>
                      <td className="px-5 py-3">{p.name}</td>
                      <td className="px-5 py-3">{p.position}</td>
                      <td className="px-5 py-3">{p.age}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div id="matches" className="mb-8 border border-white/10 bg-navy">
            <div className="border-b border-white/10 px-5 py-4 font-display text-2xl tracking-wide">
              Matches
            </div>
            <div className="divide-y divide-white/10">
              {matches.map((m) => (
                <div
                  key={m.id}
                  className="flex flex-wrap items-center justify-between gap-3 px-5 py-3 text-sm"
                >
                  <span>
                    Viking FC vs {m.opponent}
                  </span>
                  <span className="text-white/50">{m.date}</span>
                  <span className="uppercase text-red">{m.status}</span>
                </div>
              ))}
            </div>
          </div>

          <div id="news" className="border border-white/10 bg-navy">
            <div className="border-b border-white/10 px-5 py-4 font-display text-2xl tracking-wide">
              News
            </div>
            <div className="divide-y divide-white/10">
              {news.map((n) => (
                <div key={n.id} className="px-5 py-3 text-sm">
                  <p className="font-semibold">{n.title}</p>
                  <p className="text-white/50">
                    {n.category} · {n.date}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

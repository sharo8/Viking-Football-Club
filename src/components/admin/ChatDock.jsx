"use client";

import { useEffect, useState } from "react";
import { MessageCircle } from "lucide-react";
import { api, getData } from "@/services/api";

export function ChatDock() {
  const [open, setOpen] = useState(false);
  const [inbox, setInbox] = useState({ unread: 0, conversations: [] });
  const [active, setActive] = useState(null);
  const [history, setHistory] = useState([]);
  const [text, setText] = useState("");

  useEffect(() => {
    let stop = false;
    const load = () => getData("/api/chat").then((data) => { if (!stop) setInbox(data); }).catch(() => {});
    load();
    const timer = window.setInterval(load, 8000);
    return () => { stop = true; window.clearInterval(timer); };
  }, []);

  useEffect(() => {
    if (!active) return undefined;
    let stop = false;
    const load = () => getData(`/api/chat/${active}/messages`).then((rows) => { if (!stop) setHistory(rows || []); }).catch(() => {});
    load();
    api.post(`/api/chat/${active}/read`).catch(() => {});
    const timer = window.setInterval(load, 4000);
    return () => { stop = true; window.clearInterval(timer); };
  }, [active]);

  async function send(event) {
    event.preventDefault();
    if (!text.trim() || !active) return;
    const sent = await api.post(`/api/chat/${active}/messages`, { body: text });
    setHistory((rows) => [...rows, sent.data.data]);
    setText("");
  }

  const current = inbox.conversations.find((row) => row.id === active);

  return (
    <div className="chat-dock">
      <button type="button" className="icon-btn" aria-label="Messages" onClick={() => setOpen((value) => !value)}>
        <MessageCircle size={18} />
        {inbox.unread > 0 ? <span className="badge-dot">{inbox.unread}</span> : null}
      </button>
      {open ? (
        <div className="chat-panel" role="dialog" aria-label="Messages">
          <aside>
            {inbox.conversations.map((row) => (
              <button key={row.id} type="button" className={row.id === active ? "is-on" : ""} onClick={() => setActive(row.id)}>
                <strong>{row.title}</strong>
                <small>{row.preview}</small>
                {row.unread > 0 ? <em>{row.unread}</em> : null}
              </button>
            ))}
            {inbox.conversations.length === 0 ? <p>No conversations yet.</p> : null}
          </aside>
          <section>
            <header>{current?.title || "Select a conversation"}</header>
            <div className="chat-stream">
              {history.map((message) => (
                <p key={message.id} className={message.mine ? "mine" : ""}>
                  {message.body}
                  <small>{message.status === "SENT" ? "✓" : "✓✓"}</small>
                </p>
              ))}
            </div>
            <form onSubmit={send}>
              <input value={text} onChange={(event) => setText(event.target.value)} placeholder="Write a message" disabled={!active} />
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}

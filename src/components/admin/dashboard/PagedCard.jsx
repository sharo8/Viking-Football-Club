"use client";

import { useEffect, useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";

export function PagedCard({ title, tabs, pageSize = 5, queryKey, resetKey, fetcher, renderItem, empty, enabled = true }) {
  const reduced = useReducedMotion();
  const [tab, setTab] = useState(tabs?.[0]?.id || "season");
  const [page, setPage] = useState(0);
  const [dir, setDir] = useState(1);
  const query = useQuery({
    queryKey: [...queryKey, tab, page, pageSize],
    queryFn: () => fetcher({ page, size: pageSize, scope: tab }),
    enabled,
    placeholderData: keepPreviousData,
  });
  const data = query.data;
  const rows = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const current = totalPages === 0 ? 0 : (data?.page ?? page) + 1;

  useEffect(() => {
    setPage(0);
  }, [tab, resetKey]);

  const go = (next) => {
    const main = document.querySelector(".admin-main");
    const top = main ? main.scrollTop : 0;
    setDir(next > page ? 1 : -1);
    setPage(next);
    const hold = () => { if (main) main.scrollTop = top; };
    requestAnimationFrame(hold);
    window.setTimeout(hold, 60);
  };

  const onKey = (event) => {
    if (event.key === "ArrowRight" && page + 1 < totalPages) go(page + 1);
    if (event.key === "ArrowLeft" && page > 0) go(page - 1);
  };

  return (
    <section className="dash-card paged-card" aria-label={title} onKeyDown={onKey}>
      <header className="paged-card__head">
        <h2>{title}</h2>
        {tabs?.length ? (
          <div className="dash-tabs" role="tablist">
            {tabs.map((item) => (
              <button key={item.id} type="button" className={tab === item.id ? "is-on" : ""} aria-pressed={tab === item.id} onClick={() => setTab(item.id)}>
                {item.label}
              </button>
            ))}
          </div>
        ) : null}
      </header>
      <div className="paged-card__body">
        {query.isLoading ? Array.from({ length: pageSize }, (_, index) => <div key={index} className="dash-skel dash-skel--row" />) : null}
        {query.isError ? <button type="button" className="dash-retry" onClick={() => query.refetch()}>{query.error.message}</button> : null}
        {query.isSuccess && !rows.length ? empty : null}
        {rows.length ? (
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={`${tab}-${page}`}
              initial={reduced ? false : { x: dir * 28, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              exit={reduced ? undefined : { x: dir * -28, opacity: 0 }}
              transition={{ duration: reduced ? 0 : 0.28 }}
            >
              {rows.map((row, index) => renderItem(row, index))}
            </motion.div>
          </AnimatePresence>
        ) : null}
      </div>
      <footer className="paged-card__foot">
        <button type="button" className="page-btn" aria-label="Previous page" disabled={page <= 0 || query.isFetching} onMouseDown={(event) => event.preventDefault()} onClick={() => go(page - 1)}>
          <ChevronLeft size={16} /> Prev
        </button>
        <span>
          {current} / {Math.max(totalPages, current ? 1 : 0)}
          <span className="page-dots" aria-hidden="true">
            {Array.from({ length: Math.min(totalPages, 6) }, (_, index) => <i key={index} className={index === Math.min(page, 5) ? "is-on" : ""} />)}
          </span>
        </span>
        <button type="button" className="page-btn" aria-label="Next page" disabled={totalPages === 0 || page + 1 >= totalPages || query.isFetching} onMouseDown={(event) => event.preventDefault()} onClick={() => go(page + 1)}>
          Next <ChevronRight size={16} />
        </button>
      </footer>
    </section>
  );
}

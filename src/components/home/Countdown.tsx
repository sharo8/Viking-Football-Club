"use client";

import { useEffect, useState } from "react";

type Props = {
  targetDate: string;
  labels: {
    days: string;
    hours: string;
    minutes: string;
    seconds: string;
  };
};

function getDiff(target: string) {
  const total = Math.max(0, new Date(target).getTime() - Date.now());
  const days = Math.floor(total / (1000 * 60 * 60 * 24));
  const hours = Math.floor((total / (1000 * 60 * 60)) % 24);
  const minutes = Math.floor((total / (1000 * 60)) % 60);
  const seconds = Math.floor((total / 1000) % 60);
  return { days, hours, minutes, seconds };
}

export function Countdown({ targetDate, labels }: Props) {
  const [time, setTime] = useState(getDiff(targetDate));

  useEffect(() => {
    const id = setInterval(() => setTime(getDiff(targetDate)), 1000);
    return () => clearInterval(id);
  }, [targetDate]);

  const items = [
    { value: time.days, label: labels.days },
    { value: time.hours, label: labels.hours },
    { value: time.minutes, label: labels.minutes },
    { value: time.seconds, label: labels.seconds },
  ];

  return (
    <div className="grid grid-cols-4 gap-2 sm:gap-3">
      {items.map((item) => (
        <div key={item.label} className="countdown-cell">
          <p
            key={`${item.label}-${item.value}`}
            className="countdown-digit font-display text-xl text-white sm:text-3xl"
          >
            {String(item.value).padStart(2, "0")}
          </p>
          <p className="mt-1 font-data text-[9px] font-semibold uppercase tracking-[0.14em] text-white/55">
            {item.label}
          </p>
        </div>
      ))}
    </div>
  );
}

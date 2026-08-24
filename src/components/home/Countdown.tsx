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
        <div
          key={item.label}
          className="border border-white/15 bg-navy-deep/70 px-2 py-3 text-center backdrop-blur-md"
        >
          <p className="font-display text-2xl text-white sm:text-4xl">
            {String(item.value).padStart(2, "0")}
          </p>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.16em] text-white/55">
            {item.label}
          </p>
        </div>
      ))}
    </div>
  );
}

"use client";

import { useEffect, useRef } from "react";

const LINK_DISTANCE = 110;

function makeDot(width, height) {
  return {
    x: Math.random() * width,
    y: Math.random() * height,
    r: Math.random() * 1.5 + 0.55,
    vy: -(Math.random() * 0.28 + 0.07),
    vx: (Math.random() - 0.5) * 0.12,
    alpha: Math.random() * 0.45 + 0.22,
    white: Math.random() > 0.7,
  };
}

export default function ParticlesBackground() {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return undefined;
    const context = canvas.getContext("2d");
    if (!context) return undefined;

    const media = window.matchMedia("(max-width: 900px)");
    const pointer = { x: 0, y: 0 };
    let dots = [];
    let frame = 0;
    let alive = true;

    const countFor = () => (media.matches ? 25 : 60);

    const resize = () => {
      const parent = canvas.parentElement || canvas;
      const width = parent.clientWidth;
      const height = parent.clientHeight;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = Math.max(1, Math.floor(width * dpr));
      canvas.height = Math.max(1, Math.floor(height * dpr));
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      context.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    const seed = () => {
      const width = canvas.clientWidth || 1;
      const height = canvas.clientHeight || 1;
      dots = Array.from({ length: countFor() }, () => makeDot(width, height));
    };

    const draw = () => {
      const width = canvas.clientWidth;
      const height = canvas.clientHeight;
      context.clearRect(0, 0, width, height);
      const shiftX = pointer.x * 16;
      const shiftY = pointer.y * 10;

      for (const dot of dots) {
        dot.y += dot.vy;
        dot.x += dot.vx;
        if (dot.y < -12) {
          dot.y = height + 8;
          dot.x = Math.random() * width;
        }
        if (dot.x < -12) dot.x = width + 8;
        if (dot.x > width + 12) dot.x = -8;
      }

      for (let i = 0; i < dots.length; i += 1) {
        for (let j = i + 1; j < dots.length; j += 1) {
          const a = dots[i];
          const b = dots[j];
          const dist = Math.hypot(a.x - b.x, a.y - b.y);
          if (dist < LINK_DISTANCE) {
            context.strokeStyle = `rgba(163, 40, 58, ${(1 - dist / LINK_DISTANCE) * 0.28})`;
            context.lineWidth = 1;
            context.beginPath();
            context.moveTo(a.x + shiftX, a.y + shiftY);
            context.lineTo(b.x + shiftX, b.y + shiftY);
            context.stroke();
          }
        }
      }

      for (const dot of dots) {
        context.beginPath();
        context.fillStyle = dot.white
          ? `rgba(241, 243, 245, ${dot.alpha})`
          : `rgba(224, 112, 127, ${dot.alpha})`;
        context.shadowColor = dot.white ? "rgba(241,243,245,0.65)" : "rgba(163,40,58,0.85)";
        context.shadowBlur = 8;
        context.arc(dot.x + shiftX, dot.y + shiftY, dot.r, 0, Math.PI * 2);
        context.fill();
      }
      context.shadowBlur = 0;
    };

    const loop = () => {
      if (!alive) return;
      if (document.hidden) {
        frame = 0;
        return;
      }
      draw();
      frame = window.requestAnimationFrame(loop);
    };

    const onResize = () => {
      resize();
      seed();
    };

    const onPointer = (event) => {
      const rect = canvas.getBoundingClientRect();
      if (!rect.width || !rect.height) return;
      pointer.x = (event.clientX - rect.left) / rect.width - 0.5;
      pointer.y = (event.clientY - rect.top) / rect.height - 0.5;
    };

    const onVisibility = () => {
      if (!document.hidden && alive && !frame) loop();
    };

    resize();
    seed();
    loop();

    window.addEventListener("resize", onResize);
    window.addEventListener("pointermove", onPointer);
    document.addEventListener("visibilitychange", onVisibility);

    return () => {
      alive = false;
      if (frame) window.cancelAnimationFrame(frame);
      window.removeEventListener("resize", onResize);
      window.removeEventListener("pointermove", onPointer);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  return <canvas ref={canvasRef} className="login-particles" aria-hidden="true" />;
}

"use client";

import { useState } from "react";
import { Avatar } from "@/ui/kit";

const TYPES = ["image/jpeg", "image/png", "image/webp"];

async function circleFile(file) {
  if (!TYPES.includes(file.type)) throw new Error("type");
  if (file.size > 5 * 1024 * 1024) throw new Error("size");
  const bitmap = await createImageBitmap(file);
  const size = Math.min(bitmap.width, bitmap.height);
  const canvas = document.createElement("canvas");
  canvas.width = 512;
  canvas.height = 512;
  const ctx = canvas.getContext("2d");
  ctx.beginPath();
  ctx.arc(256, 256, 256, 0, Math.PI * 2);
  ctx.closePath();
  ctx.clip();
  ctx.drawImage(bitmap, (bitmap.width - size) / 2, (bitmap.height - size) / 2, size, size, 0, 0, 512, 512);
  const blob = await new Promise((resolve) => canvas.toBlob(resolve, "image/png"));
  return new File([blob], "photo.png", { type: "image/png" });
}

export function PhotoUpload({ name, src, hint, dropLabel, onFile, onError }) {
  const [preview, setPreview] = useState(src || "");
  const [over, setOver] = useState(false);

  const take = async (file) => {
    if (!file) return;
    try {
      const cropped = await circleFile(file);
      setPreview(URL.createObjectURL(cropped));
      onFile(cropped);
    } catch (err) {
      onError(err?.message === "size" || err?.message === "type" ? hint : hint);
    }
  };

  return (
    <label
      className={`photo-drop${over ? " is-over" : ""}`}
      onDragOver={(event) => { event.preventDefault(); setOver(true); }}
      onDragLeave={() => setOver(false)}
      onDrop={(event) => { event.preventDefault(); setOver(false); take(event.dataTransfer.files?.[0]); }}
    >
      <Avatar name={name || "?"} src={preview} size="96" />
      <span>{dropLabel}</span>
      <small>{hint}</small>
      <input type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(event) => take(event.target.files?.[0])} />
    </label>
  );
}

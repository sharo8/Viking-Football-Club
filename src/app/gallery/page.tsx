"use client";

import { useMemo, useState } from "react";
import Image from "next/image";
import { galleryItems, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";
import { Download, Play } from "lucide-react";

type Filter =
  | "all"
  | "photos"
  | "videos"
  | "moments"
  | "trophies"
  | "fans"
  | "training";

export default function GalleryPage() {
  const { t } = useLocale();
  const [filter, setFilter] = useState<Filter>("all");

  const filters: { key: Filter; label: string }[] = [
    { key: "all", label: t.news.categories.all },
    { key: "photos", label: t.gallery.photos },
    { key: "videos", label: t.gallery.videos },
    { key: "moments", label: t.gallery.moments },
    { key: "trophies", label: t.gallery.trophies },
    { key: "fans", label: t.gallery.fans },
    { key: "training", label: t.gallery.training },
  ];

  const list = useMemo(() => {
    if (filter === "all") return galleryItems;
    if (filter === "photos" || filter === "videos") {
      return galleryItems.filter((g) => g.type === filter);
    }
    return galleryItems.filter((g) => g.category === filter);
  }, [filter]);

  return (
    <>
      <PageHero title={t.gallery.title} image={images.pitch} />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <div className="mb-8 flex flex-wrap gap-2">
            {filters.map((f) => (
              <button
                key={f.key}
                type="button"
                onClick={() => setFilter(f.key)}
                className={`px-3 py-2 text-[11px] font-bold uppercase tracking-[0.12em] ${
                  filter === f.key
                    ? "bg-red text-white"
                    : "border border-[var(--border)]"
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {list.map((item) => (
              <figure
                key={item.id}
                className="group relative aspect-[16/11] overflow-hidden border border-[var(--border)]"
              >
                <Image
                  src={item.image}
                  alt={item.title}
                  fill
                  className="object-cover transition duration-500 group-hover:scale-105"
                  sizes="(max-width:768px) 100vw, 33vw"
                />
                <figcaption className="absolute inset-x-0 bottom-0 flex items-center justify-between bg-gradient-to-t from-navy-deep to-transparent p-4 text-white opacity-0 transition group-hover:opacity-100">
                  <div>
                    <p className="font-display text-xl tracking-wide">
                      {item.title}
                    </p>
                    <p className="text-[11px] uppercase tracking-wider text-white/70">
                      {item.type}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    {item.type === "videos" && (
                      <span className="rounded-full bg-red p-2">
                        <Play size={16} />
                      </span>
                    )}
                    <a
                      href={item.image}
                      download
                      className="rounded-full bg-white/20 p-2 hover:bg-red"
                      aria-label="Download"
                    >
                      <Download size={16} />
                    </a>
                  </div>
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

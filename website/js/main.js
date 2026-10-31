/* Viking FC — Main JavaScript */

const ICONS = {
  search: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>`,
  sun: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/></svg>`,
  moon: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`,
  menu: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg>`,
  close: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12"/></svg>`,
  chevron: `<svg class="icon icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>`,
  chevronLeft: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6"/></svg>`,
  chevronRight: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 18 6-6-6-6"/></svg>`,
  instagram: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="2" width="20" height="20" rx="5"/><path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/><line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/></svg>`,
  facebook: `<svg class="icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M14 13.5h2.5l1-4H14v-2c0-1.03 0-2 2-2h1.5V2.14c-.326-.043-1.557-.14-2.857-.14C11.928 2 10 3.657 10 6.7v2.8H7v4h3V22h4z"/></svg>`,
  youtube: `<svg class="icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.5 12 3.5 12 3.5s-7.5 0-9.4.6A3 3 0 0 0 .5 6.2 31.5 31.5 0 0 0 0 12a31.5 31.5 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.6 9.4.6 9.4.6s7.5 0 9.4-.6a3 3 0 0 0 2.1-2.1A31.5 31.5 0 0 0 24 12a31.5 31.5 0 0 0-.5-5.8zM9.75 15.5v-7l6.5 3.5-6.5 3.5z"/></svg>`,
  whatsapp: `<svg class="icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M17.5 14.4c-.3-.1-1.6-.8-1.8-.9-.2-.1-.4-.1-.6.1-.2.3-.7.9-.8 1-.2.1-.3.2-.6.1s-1.2-.4-2.3-1.5c-.9-.8-1.4-1.7-1.6-2-.2-.3 0-.4.1-.6l.5-.6c.1-.2.2-.3.3-.5.1-.2 0-.4 0-.5-.1-.1-.6-1.4-.8-1.9-.2-.5-.4-.4-.6-.4h-.5c-.2 0-.5.1-.7.3-.2.3-.9.8-.9 2.1s.9 2.4 1 2.6c.1.2 1.8 2.8 4.4 3.9 1.6.7 2.2.8 3 .6.5-.1 1.6-.6 1.8-1.3.2-.6.2-1.2.1-1.3-.1-.1-.3-.2-.6-.3z"/><path d="M12 2a10 10 0 0 0-8.7 14.9L2 22l5.2-1.3A10 10 0 1 0 12 2zm0 18.2a8.2 8.2 0 0 1-4.2-1.1l-.3-.2-3.1.8.8-3-.2-.3A8.2 8.2 0 1 1 12 20.2z"/></svg>`,
  tiktok: `<svg class="icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M19.6 6.7a4.8 4.8 0 0 1-3.8-4.2V2h-3.4v13.7a2.9 2.9 0 1 1-2.1-2.8v-3.5a6.3 6.3 0 1 0 5.5 6.3V8.9a8.2 8.2 0 0 0 4.8 1.5V6.9a4.9 4.9 0 0 1-1-.2z"/></svg>`,
  mapPin: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>`,
  phone: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6 19.8 19.8 0 0 1-3.1-8.7A2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.6a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.5-1.2a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.6.7A2 2 0 0 1 22 16.9z"/></svg>`,
  mail: `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>`,
};

function socialLinksHtml() {
  return `
    <a href="${CLUB.social.instagram}" target="_blank" rel="noreferrer" aria-label="Instagram">${ICONS.instagram}</a>
    <a href="${CLUB.social.tiktok}" target="_blank" rel="noreferrer" aria-label="TikTok">${ICONS.tiktok}</a>
    <a href="${CLUB.social.facebook}" target="_blank" rel="noreferrer" aria-label="Facebook">${ICONS.facebook}</a>
    <a href="${CLUB.social.youtube}" target="_blank" rel="noreferrer" aria-label="YouTube">${ICONS.youtube}</a>
    <a href="${CLUB.social.whatsapp}" target="_blank" rel="noreferrer" aria-label="WhatsApp">${ICONS.whatsapp}</a>
  `;
}

function themeIconHtml() {
  const isDark = document.documentElement.classList.contains("dark");
  return isDark ? ICONS.sun : ICONS.moon;
}

function langLabel(code) {
  const map = {
    en: `EN ${ICONS.chevron}`,
    fr: `FR ${ICONS.chevron}`,
    rw: `RW ${ICONS.chevron}`,
  };
  return map[code] || map.en;
}

function isAdminPage() {
  return window.location.pathname.replace(/\\/g, "/").includes("admin.html");
}

function currentPage() {
  const path = window.location.pathname.replace(/\\/g, "/");
  const file = path.split("/").pop() || "index.html";
  return file || "index.html";
}

function buildNav() {
  if (isAdminPage()) return;

  const tr = t();
  const links = [
    { file: "index.html", key: "home" },
    { file: "about.html", key: "about" },
    { file: "teams.html", key: "teams" },
    { file: "matches.html", key: "matches" },
    { file: "players.html", key: "players" },
    { file: "management.html", key: "management" },
    { file: "news.html", key: "news" },
    { file: "gallery.html", key: "gallery" },
    { file: "services.html", key: "services" },
    { file: "shop.html", key: "shop" },
    { file: "contact.html", key: "contact" },
  ];

  const page = currentPage();
  const logo = asset(CLUB.logo);

  const linkHtml = links
    .map((l) => {
      const href = pageLink(l.file === "index.html" ? "index.html" : l.file);
      const active =
        (l.file === "index.html" && (page === "" || page === "index.html")) ||
        page === l.file ||
        (l.file === "players.html" && page === "player.html") ||
        (l.file === "news.html" && page === "news-detail.html");
      return `<a href="${href}" class="${active ? "active" : ""}" data-i18n="nav.${l.key}">${tr.nav[l.key]}</a>`;
    })
    .join("");

  const nav = document.createElement("header");
  nav.className = "navbar";
  nav.id = "navbar";
  nav.innerHTML = `
    <div class="container nav-inner">
      <a class="brand" href="${pageLink("index.html")}">
        <img src="${logo}" alt="Viking FC" />
        <div>
          <div class="brand-text">VIKING FC</div>
          <div class="brand-sub" data-i18n="brand.official">Official Club</div>
        </div>
      </a>
      <nav class="nav-links" aria-label="Main">${linkHtml}</nav>
      <div class="nav-actions">
        <button class="icon-btn" id="searchToggle" type="button" title="Search" aria-label="Search">${ICONS.search}</button>
        <button class="icon-btn" id="themeToggle" type="button" title="Toggle theme" aria-label="Toggle theme">${themeIconHtml()}</button>
        <div class="lang-wrap">
          <button class="lang-btn" id="langBtn" type="button" aria-label="Language" aria-haspopup="true">${langLabel(getLocale())}</button>
          <div class="lang-menu" id="langMenu" role="menu">
            <button type="button" data-lang="en" role="menuitem">English</button>
            <button type="button" data-lang="fr" role="menuitem">Français</button>
            <button type="button" data-lang="rw" role="menuitem">Kinyarwanda</button>
          </div>
        </div>
        <button class="menu-toggle icon-btn" id="menuToggle" type="button" aria-label="Open menu">${ICONS.menu}</button>
      </div>
    </div>
    <div class="search-bar" id="searchBar">
      <div class="container">
        <input type="search" placeholder="${tr.nav.search}..." aria-label="Search" />
      </div>
    </div>
    <nav class="mobile-nav" id="mobileNav" aria-label="Mobile">${linkHtml}</nav>
  `;
  document.body.prepend(nav);
}

function buildFooter() {
  if (isAdminPage()) return;
  const tr = t();
  const footer = document.createElement("footer");
  footer.className = "footer";
  footer.innerHTML = `
    <div class="container section footer-grid">
      <div>
        <div class="brand footer-brand">
          <img src="${asset(CLUB.logo)}" alt="Viking FC" />
          <div>
            <div class="brand-text">VIKING FC</div>
            <div class="brand-sub">${CLUB.slogan}</div>
          </div>
        </div>
        <p class="footer-tagline" data-i18n="brand.tagline">Professional football. Family spirit. European ambition.</p>
      </div>
      <div>
        <h3 data-i18n="footer.quickLinks">${tr.footer.quickLinks}</h3>
        <ul class="footer-links">
          <li><a href="${pageLink("about.html")}" data-i18n="nav.about">${tr.nav.about}</a></li>
          <li><a href="${pageLink("players.html")}" data-i18n="nav.players">${tr.nav.players}</a></li>
          <li><a href="${pageLink("matches.html")}" data-i18n="nav.matches">${tr.nav.matches}</a></li>
          <li><a href="${pageLink("news.html")}" data-i18n="nav.news">${tr.nav.news}</a></li>
          <li><a href="${pageLink("shop.html")}" data-i18n="nav.shop">${tr.nav.shop}</a></li>
          <li><a href="${pageLink("contact.html")}" data-i18n="nav.contact">${tr.nav.contact}</a></li>
        </ul>
      </div>
      <div>
        <h3 data-i18n="sponsors.title">${tr.sponsors.title}</h3>
        <div class="sponsor-list footer-sponsors">
          ${SPONSORS.slice(0, 4).map((s) => `<div class="sponsor-item">${s}</div>`).join("")}
        </div>
        <div class="footer-contact mt-3">
          <p><a href="tel:${CLUB.phone}">${CLUB.phone}</a></p>
          <p><a href="mailto:${CLUB.email}">${CLUB.email}</a></p>
        </div>
      </div>
      <div>
        <h3 data-i18n="newsletter.title">${tr.newsletter.title}</h3>
        <form class="newsletter-form" onsubmit="event.preventDefault();alert('Subscribed!');">
          <input type="email" required placeholder="${tr.newsletter.placeholder}" data-i18n-placeholder="newsletter.placeholder" />
          <button class="btn btn-primary" type="submit" data-i18n="newsletter.subscribe">${tr.newsletter.subscribe}</button>
        </form>
        <p class="footer-follow" data-i18n="footer.follow">${tr.footer.follow}</p>
        <div class="socials">${socialLinksHtml()}</div>
      </div>
    </div>
    <div class="footer-bottom" data-i18n="footer.copyright">${tr.footer.copyright}</div>
  `;
  document.body.appendChild(footer);
}

function applyTranslations() {
  const tr = t();
  document.documentElement.lang = getLocale() === "rw" ? "rw" : getLocale();

  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const path = el.getAttribute("data-i18n");
    const val = trPath(path);
    if (typeof val === "string" && val !== path) el.textContent = val;
  });

  document.querySelectorAll("[data-i18n-placeholder]").forEach((el) => {
    const path = el.getAttribute("data-i18n-placeholder");
    const val = trPath(path);
    if (typeof val === "string" && val !== path) el.placeholder = val;
  });

  const langBtn = document.getElementById("langBtn");
  if (langBtn) langBtn.innerHTML = langLabel(getLocale());

  const themeBtn = document.getElementById("themeToggle");
  if (themeBtn) themeBtn.innerHTML = themeIconHtml();

  // Mark active language in menu
  document.querySelectorAll("#langMenu [data-lang]").forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute("data-lang") === getLocale());
  });
}

function initTheme() {
  const saved = localStorage.getItem("viking-theme") || "dark";
  const isDark = saved === "dark";
  document.documentElement.classList.toggle("dark", isDark);
  document.documentElement.style.colorScheme = isDark ? "dark" : "light";
}

function toggleTheme() {
  const isDark = document.documentElement.classList.toggle("dark");
  localStorage.setItem("viking-theme", isDark ? "dark" : "light");
  document.documentElement.style.colorScheme = isDark ? "dark" : "light";
  const themeBtn = document.getElementById("themeToggle");
  if (themeBtn) themeBtn.innerHTML = themeIconHtml();
}

function initNavEvents() {
  const navbar = document.getElementById("navbar");
  if (!navbar) return;

  const menuToggle = document.getElementById("menuToggle");

  window.addEventListener("scroll", () => {
    navbar.classList.toggle("scrolled", window.scrollY > 24);
  });

  menuToggle?.addEventListener("click", () => {
    const open = navbar.classList.toggle("open");
    menuToggle.innerHTML = open ? ICONS.close : ICONS.menu;
    menuToggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
    document.body.classList.toggle("nav-open", open);
  });

  document.getElementById("mobileNav")?.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
      navbar.classList.remove("open");
      document.body.classList.remove("nav-open");
      if (menuToggle) {
        menuToggle.innerHTML = ICONS.menu;
        menuToggle.setAttribute("aria-label", "Open menu");
      }
    });
  });

  document.getElementById("searchToggle")?.addEventListener("click", () => {
    document.getElementById("searchBar")?.classList.toggle("show");
  });

  document.getElementById("themeToggle")?.addEventListener("click", toggleTheme);

  document.getElementById("langBtn")?.addEventListener("click", (e) => {
    e.stopPropagation();
    document.getElementById("langMenu")?.classList.toggle("show");
  });

  document.querySelectorAll("#langMenu [data-lang]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setLocale(btn.getAttribute("data-lang"));
      document.getElementById("langMenu")?.classList.remove("show");
    });
  });

  document.addEventListener("click", () => {
    document.getElementById("langMenu")?.classList.remove("show");
  });
}

let countdownTimer = null;

function startCountdown(targetDate, rootId) {
  const root = document.getElementById(rootId);
  if (!root) return;

  function tick() {
    const tr = t();
    const total = Math.max(0, new Date(targetDate).getTime() - Date.now());
    const days = Math.floor(total / (1000 * 60 * 60 * 24));
    const hours = Math.floor((total / (1000 * 60 * 60)) % 24);
    const minutes = Math.floor((total / (1000 * 60)) % 60);
    const seconds = Math.floor((total / 1000) % 60);
    const items = [
      [days, tr.match.days],
      [hours, tr.match.hours],
      [minutes, tr.match.minutes],
      [seconds, tr.match.seconds],
    ];
    root.innerHTML = items
      .map(
        ([v, label]) => `
      <div class="countdown-item">
        <strong>${String(v).padStart(2, "0")}</strong>
        <span>${label}</span>
      </div>`,
      )
      .join("");
  }

  tick();
  if (countdownTimer) clearInterval(countdownTimer);
  countdownTimer = setInterval(tick, 1000);
}

function qs(name) {
  return new URLSearchParams(window.location.search).get(name);
}

/* ========== LIGHTBOX ========== */
let lightboxState = { items: [], index: 0 };

function ensureLightbox() {
  if (document.getElementById("lightbox")) return;
  const box = document.createElement("div");
  box.id = "lightbox";
  box.className = "lightbox";
  box.setAttribute("role", "dialog");
  box.setAttribute("aria-modal", "true");
  box.innerHTML = `
    <button type="button" class="lightbox-close" id="lightboxClose" aria-label="Close">${ICONS.close}</button>
    <button type="button" class="lightbox-nav lightbox-prev" id="lightboxPrev" aria-label="Previous">${ICONS.chevronLeft}</button>
    <button type="button" class="lightbox-nav lightbox-next" id="lightboxNext" aria-label="Next">${ICONS.chevronRight}</button>
    <div class="lightbox-inner">
      <div class="lightbox-img-wrap">
        <img id="lightboxImg" src="" alt="" />
        <video id="lightboxVideo" controls playsinline preload="metadata" hidden></video>
      </div>
      <div class="lightbox-title" id="lightboxTitle"></div>
      <div class="lightbox-meta" id="lightboxMeta"></div>
      <div class="lightbox-actions">
        <a id="lightboxDownload" href="#" download>Download</a>
      </div>
    </div>
  `;
  document.body.appendChild(box);

  document.getElementById("lightboxClose").addEventListener("click", closeLightbox);
  document.getElementById("lightboxPrev").addEventListener("click", () => stepLightbox(-1));
  document.getElementById("lightboxNext").addEventListener("click", () => stepLightbox(1));
  box.addEventListener("click", (e) => {
    if (e.target === box) closeLightbox();
  });
  document.addEventListener("keydown", (e) => {
    if (!box.classList.contains("open")) return;
    if (e.key === "Escape") closeLightbox();
    if (e.key === "ArrowLeft") stepLightbox(-1);
    if (e.key === "ArrowRight") stepLightbox(1);
  });
}

function openLightbox(items, index) {
  ensureLightbox();
  lightboxState.items = items;
  lightboxState.index = index;
  renderLightbox();
  document.getElementById("lightbox").classList.add("open");
  document.body.classList.add("lightbox-open");
}

function closeLightbox() {
  const video = document.getElementById("lightboxVideo");
  if (video) {
    video.pause();
    video.removeAttribute("src");
    video.load();
    video.hidden = true;
  }
  const img = document.getElementById("lightboxImg");
  if (img) img.hidden = false;
  document.getElementById("lightbox")?.classList.remove("open");
  document.body.classList.remove("lightbox-open");
}

function stepLightbox(dir) {
  const len = lightboxState.items.length;
  if (!len) return;
  lightboxState.index = (lightboxState.index + dir + len) % len;
  renderLightbox();
}

function renderLightbox() {
  const item = lightboxState.items[lightboxState.index];
  if (!item) return;
  const img = document.getElementById("lightboxImg");
  const video = document.getElementById("lightboxVideo");
  const dl = document.getElementById("lightboxDownload");

  if (item.video) {
    img.hidden = true;
    video.hidden = false;
    video.pause();
    video.src = item.video;
    video.poster = item.src || "";
    video.load();
    dl.href = item.video;
  } else {
    video.pause();
    video.removeAttribute("src");
    video.hidden = true;
    img.hidden = false;
    img.src = item.src;
    img.alt = item.title || "";
    dl.href = item.src;
  }

  document.getElementById("lightboxTitle").textContent = item.title || "";
  document.getElementById("lightboxMeta").textContent = item.meta || `${lightboxState.index + 1} / ${lightboxState.items.length}`;
  dl.setAttribute("download", "");
}

function bindGalleryLightbox(rootSelector = "#galleryGrid") {
  const root = document.querySelector(rootSelector);
  if (!root || root.dataset.lightboxBound) return;
  root.dataset.lightboxBound = "1";

  root.addEventListener("click", (e) => {
    const item = e.target.closest(".gallery-item");
    if (!item) return;
    if (e.target.closest("a[download]")) return;

    const nodes = [...root.querySelectorAll(".gallery-item")];
    const items = nodes.map((node) => {
      const img = node.querySelector("img");
      const title = node.querySelector(".gallery-caption .font-display")?.textContent || img?.alt || "";
      const meta = node.querySelector(".gallery-caption div div:last-child")?.textContent || "";
      return {
        src: node.dataset.src || img?.src,
        video: node.dataset.video || "",
        title,
        meta,
      };
    });
    const index = nodes.indexOf(item);
    openLightbox(items, index);
  });
}

let scrollObserver = null;

function initScrollAnimations() {
  if (typeof IntersectionObserver === "undefined") return;
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    document.querySelectorAll(".reveal, .reveal-fade, .reveal-scale").forEach((el) => {
      el.classList.add("is-visible");
    });
    return;
  }

  if (scrollObserver) {
    scrollObserver.disconnect();
  }

  scrollObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
        } else {
          entry.target.classList.remove("is-visible");
        }
      });
    },
    {
      root: null,
      rootMargin: "0px 0px -8% 0px",
      threshold: 0.12,
    },
  );

  const selectors = [
    ".section-head",
    ".section > .container > .section-label",
    ".section > .container > .heading-lg",
    ".news-card",
    ".player-card",
    ".staff-card",
    ".service-card",
    ".product-card",
    ".team-card",
    ".video-card",
    ".gallery-item",
    ".match-list-item",
    ".fixture-row",
    ".result-row",
    ".table-wrap",
    ".split",
    ".quote",
    ".contact-grid > *",
    ".sponsor-item",
    ".season-stat",
    ".countdown-item",
    ".match-day-grid > *",
  ];

  document.querySelectorAll(selectors.join(",")).forEach((el) => {
    if (
      !el.classList.contains("reveal") &&
      !el.classList.contains("reveal-fade") &&
      !el.classList.contains("reveal-scale")
    ) {
      if (
        el.classList.contains("news-card") ||
        el.classList.contains("player-card") ||
        el.classList.contains("staff-card") ||
        el.classList.contains("gallery-item") ||
        el.classList.contains("sponsor-item")
      ) {
        el.classList.add("reveal-scale");
      } else if (el.classList.contains("section-label") || el.classList.contains("heading-lg")) {
        el.classList.add("reveal-fade");
      } else {
        el.classList.add("reveal");
      }
    }

    const parentGrid = el.parentElement;
    if (
      parentGrid &&
      (parentGrid.classList.contains("grid-3") ||
        parentGrid.classList.contains("grid-2") ||
        parentGrid.classList.contains("grid-4") ||
        parentGrid.classList.contains("gallery-grid") ||
        parentGrid.classList.contains("video-grid") ||
        parentGrid.classList.contains("staff-grid") ||
        parentGrid.classList.contains("countdown") ||
        parentGrid.classList.contains("sponsor-list") ||
        parentGrid.classList.contains("match-day-grid"))
    ) {
      const indexInParent = Array.from(parentGrid.children).indexOf(el);
      if (indexInParent >= 0) {
        el.style.transitionDelay = `${(indexInParent % 6) * 0.07}s`;
      }
    }

    scrollObserver.observe(el);
  });
}

document.addEventListener("DOMContentLoaded", () => {
  initTheme();
  document.documentElement.lang = getLocale() === "rw" ? "rw" : getLocale();
  buildNav();
  buildFooter();
  applyTranslations();
  initNavEvents();
  if (typeof initPage === "function") initPage();
  bindGalleryLightbox();
  requestAnimationFrame(() => initScrollAnimations());

  document.addEventListener("localechange", () => {
    if (typeof initPage === "function") initPage();
    applyTranslations();
    requestAnimationFrame(() => initScrollAnimations());
  });
});

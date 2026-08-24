# Viking FC — Official Club Website

Professional football club website for **VIKING FC**.

## Stack

- **Frontend:** Next.js 15, React 19, Tailwind CSS 4, Framer Motion
- **Fonts:** Bebas Neue (display), Manrope (body), JetBrains Mono (data)
- **Brand colors:** Navy `#0B1F3A` · Red `#D90429` · Gold `#FFD700` · White · Light Gray

## Features

- Full page structure: Home, About, Teams, Matches, Players, Management, News, Gallery, Services, Shop, Contact
- Player profile pages + News detail pages
- Dark / Light mode toggle with animation
- Multilingual: English · Français · Kinyarwanda
- Hero video on Home (`public/videos/hero.mp4`)
- Match countdown, fixtures, results, league table
- Admin dashboard UI (`/admin`)
- Instagram & TikTok links in footer/contact

## Run locally

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

## Assets

Place / replace media in:

- `public/logo/logo.png` — club crest
- `public/videos/hero.mp4` — home hero video
- `public/images/` — team & training photos

Simulation photos are used where individual headshots are not available yet — replace anytime.

## Social

- Instagram: https://www.instagram.com/viking_football.club
- TikTok: https://www.tiktok.com/@fc.vikings1

## Next steps (backend)

Ready to connect to Spring Boot + MySQL + JWT:

- Players / Staff / Matches / News CRUD
- Cloudinary (images) + S3/Firebase (videos)
- Auth for members & admin

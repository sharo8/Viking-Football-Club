# Viking FC — HTML / CSS / JavaScript

Site officiel en **HTML, CSS et JavaScript** (sans React / Next.js).

## Ouvrir le site

Double-clique sur :

```
website/index.html
```

Ou avec un serveur local :

```bash
cd website
npx serve .
```

## Structure

```
website/
├── index.html          ← Accueil (vidéo hero)
├── css/style.css       ← Tous les styles
├── js/
│   ├── data.js         ← Joueurs, matchs, news...
│   ├── i18n.js         ← EN / FR / Kinyarwanda
│   └── main.js         ← Menu, thème, footer, countdown
├── pages/              ← Toutes les autres pages
├── logo/logo.png
├── images/
└── videos/hero.mp4
```

## Pages

Home · About · Teams · Matches · Players · Management · News · Gallery · Services · Shop · Contact · Login · Admin

## Fonctions

- Mode clair / sombre
- Langues EN / FR / RW
- Vidéo plein écran sur Home
- Countdown prochain match
- Instagram + TikTok dans Contact / Footer

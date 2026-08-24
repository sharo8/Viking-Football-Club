export const club = {
  name: "VIKING FC",
  slogan: "One Club. One Family. One Dream.",
  logo: "/logo/logo.png",
  email: "info@vikingfc.com",
  phone: "+250 788 000 000",
  address: "Viking Stadium, Kigali, Rwanda",
  social: {
    instagram:
      "https://www.instagram.com/viking_football.club?igsh=MXIwNTM4MWV0d211ag%3D%3D&utm_source=qr",
    tiktok: "https://www.tiktok.com/@fc.vikings1",
    facebook: "https://facebook.com/vikingfc",
    youtube: "https://youtube.com/@vikingfc",
    whatsapp: "https://wa.me/250788000000",
  },
};

export const images = {
  logo: "/logo/logo.png",
  heroVideo: "/videos/hero.mp4",
  studio: "/images/studio.png",
  whiteKit: "/images/white-kit.png",
  blueKit: "/images/blue-kit.png",
  squad: "/images/squad.png",
  portrait: "/images/portrait.png",
  pitch: "/images/pitch.png",
  jog: "/images/jog.png",
  run: "/images/run.png",
  stretch: "/images/stretch.png",
  celebrate: "/images/celebrate.png",
};

export type PlayerPosition =
  | "Goalkeeper"
  | "Defender"
  | "Midfielder"
  | "Forward";

export type Player = {
  id: string;
  slug: string;
  name: string;
  number: number;
  position: PlayerPosition;
  nationality: string;
  age: number;
  height: string;
  weight: string;
  dob: string;
  image: string;
  bio: string;
  stats: {
    appearances: number;
    goals: number;
    assists: number;
    cleanSheets?: number;
  };
};

export const players: Player[] = [
  {
    id: "1",
    slug: "eric-nshimiyimana",
    name: "Eric Nshimiyimana",
    number: 1,
    position: "Goalkeeper",
    nationality: "Rwanda",
    age: 24,
    height: "188 cm",
    weight: "82 kg",
    dob: "2002-03-14",
    image: images.portrait,
    bio: "Commanding shot-stopper and vocal leader from the back.",
    stats: { appearances: 28, goals: 0, assists: 1, cleanSheets: 12 },
  },
  {
    id: "2",
    slug: "jean-baptiste-uwase",
    name: "Jean Baptiste Uwase",
    number: 4,
    position: "Defender",
    nationality: "Rwanda",
    age: 26,
    height: "185 cm",
    weight: "78 kg",
    dob: "2000-07-22",
    image: images.pitch,
    bio: "Reliable centre-back with strong aerial ability.",
    stats: { appearances: 30, goals: 2, assists: 1 },
  },
  {
    id: "3",
    slug: "david-habimana",
    name: "David Habimana",
    number: 5,
    position: "Defender",
    nationality: "Rwanda",
    age: 23,
    height: "182 cm",
    weight: "76 kg",
    dob: "2003-01-09",
    image: images.squad,
    bio: "Modern full-back who loves to push forward.",
    stats: { appearances: 27, goals: 1, assists: 5 },
  },
  {
    id: "4",
    slug: "patrick-mugisha",
    name: "Patrick Mugisha",
    number: 3,
    position: "Defender",
    nationality: "Rwanda",
    age: 25,
    height: "180 cm",
    weight: "74 kg",
    dob: "2001-11-02",
    image: images.whiteKit,
    bio: "Tough left-back with excellent recovery pace.",
    stats: { appearances: 25, goals: 0, assists: 3 },
  },
  {
    id: "5",
    slug: "alex-iradukunda",
    name: "Alex Iradukunda",
    number: 6,
    position: "Midfielder",
    nationality: "Rwanda",
    age: 27,
    height: "178 cm",
    weight: "73 kg",
    dob: "1999-05-18",
    image: images.jog,
    bio: "Engine of the midfield — tireless and tactically sharp.",
    stats: { appearances: 31, goals: 3, assists: 7 },
  },
  {
    id: "6",
    slug: "kevin-nkurunziza",
    name: "Kevin Nkurunziza",
    number: 8,
    position: "Midfielder",
    nationality: "Rwanda",
    age: 22,
    height: "176 cm",
    weight: "70 kg",
    dob: "2004-08-30",
    image: images.run,
    bio: "Creative midfielder with a sharp final pass.",
    stats: { appearances: 29, goals: 6, assists: 11 },
  },
  {
    id: "7",
    slug: "samuel-hakizimana",
    name: "Samuel Hakizimana",
    number: 10,
    position: "Midfielder",
    nationality: "Rwanda",
    age: 24,
    height: "175 cm",
    weight: "71 kg",
    dob: "2002-12-05",
    image: images.celebrate,
    bio: "Club playmaker and set-piece specialist.",
    stats: { appearances: 30, goals: 9, assists: 14 },
  },
  {
    id: "8",
    slug: "christian-mbonyumuremyi",
    name: "Christian Mbonyumuremyi",
    number: 7,
    position: "Forward",
    nationality: "Rwanda",
    age: 23,
    height: "179 cm",
    weight: "74 kg",
    dob: "2003-04-21",
    image: images.blueKit,
    bio: "Explosive winger with deadly pace on the break.",
    stats: { appearances: 28, goals: 12, assists: 8 },
  },
  {
    id: "9",
    slug: "felix-tuyisenge",
    name: "Felix Tuyisenge",
    number: 9,
    position: "Forward",
    nationality: "Rwanda",
    age: 25,
    height: "184 cm",
    weight: "80 kg",
    dob: "2001-09-12",
    image: images.stretch,
    bio: "Clinical striker and focal point of the attack.",
    stats: { appearances: 30, goals: 18, assists: 4 },
  },
  {
    id: "10",
    slug: "olive-manzi",
    name: "Olive Manzi",
    number: 11,
    position: "Forward",
    nationality: "Rwanda",
    age: 21,
    height: "177 cm",
    weight: "72 kg",
    dob: "2005-02-28",
    image: images.studio,
    bio: "Young talent with fearless dribbling and finishing.",
    stats: { appearances: 22, goals: 8, assists: 6 },
  },
  {
    id: "11",
    slug: "bruno-gasana",
    name: "Bruno Gasana",
    number: 17,
    position: "Midfielder",
    nationality: "Rwanda",
    age: 20,
    height: "174 cm",
    weight: "69 kg",
    dob: "2006-06-15",
    image: images.pitch,
    bio: "Academy graduate rising fast into the first team.",
    stats: { appearances: 15, goals: 2, assists: 3 },
  },
  {
    id: "12",
    slug: "yves-rutayisire",
    name: "Yves Rutayisire",
    number: 19,
    position: "Defender",
    nationality: "Rwanda",
    age: 22,
    height: "183 cm",
    weight: "77 kg",
    dob: "2004-10-03",
    image: images.squad,
    bio: "Composed defender with excellent reading of the game.",
    stats: { appearances: 20, goals: 1, assists: 0 },
  },
];

export type StaffMember = {
  id: string;
  name: string;
  role: string;
  bio: string;
  image: string;
};

export const staff: StaffMember[] = [
  {
    id: "pres",
    name: "Jean Claude Mugabo",
    role: "President",
    bio: "Leading Viking FC with a clear vision: build a European-standard club rooted in Rwandan pride.",
    image: images.studio,
  },
  {
    id: "vp",
    name: "Alice Uwimana",
    role: "Vice-President",
    bio: "Oversees club governance, partnerships and long-term strategic growth.",
    image: images.squad,
  },
  {
    id: "dg",
    name: "Eric Ndayisaba",
    role: "Directeur Général",
    bio: "Responsible for day-to-day operations and institutional excellence.",
    image: images.whiteKit,
  },
  {
    id: "ds",
    name: "Patrick Habimana",
    role: "Directeur Sportif",
    bio: "Shapes recruitment, squad planning and the footballing identity of Viking FC.",
    image: images.portrait,
  },
  {
    id: "mgr",
    name: "Coach Michel Nzeyimana",
    role: "Manager",
    bio: "High-pressing football, youth development and a winning mentality.",
    image: images.pitch,
  },
  {
    id: "ac",
    name: "Thierry Niyonshuti",
    role: "Assistant Coach",
    bio: "Supports match preparation, tactics and player development sessions.",
    image: images.jog,
  },
  {
    id: "gk",
    name: "Samuel Bizimana",
    role: "Goalkeeper Coach",
    bio: "Specialist coach focused on shot-stopping, distribution and leadership.",
    image: images.blueKit,
  },
  {
    id: "fit",
    name: "Dr. Diane Mukamana",
    role: "Fitness Coach",
    bio: "Designs elite conditioning programs for peak match performance.",
    image: images.stretch,
  },
  {
    id: "doc",
    name: "Dr. Yves Kalisa",
    role: "Team Doctor",
    bio: "Ensures medical readiness and player wellbeing across the season.",
    image: images.run,
  },
  {
    id: "physio",
    name: "Grace Ingabire",
    role: "Physiotherapist",
    bio: "Injury prevention, recovery and rehabilitation specialist.",
    image: images.celebrate,
  },
  {
    id: "kit",
    name: "Paul Habineza",
    role: "Kit Manager",
    bio: "Manages matchday kits, training gear and equipment logistics.",
    image: images.whiteKit,
  },
  {
    id: "media",
    name: "Nadia Uwera",
    role: "Media Officer",
    bio: "Club communications, press relations and digital storytelling.",
    image: images.studio,
  },
  {
    id: "photo",
    name: "Chris Mutoni",
    role: "Photographer",
    bio: "Captures the Viking story through matchday and training imagery.",
    image: images.pitch,
  },
  {
    id: "sec",
    name: "Major Robert Kayitare",
    role: "Security Officer",
    bio: "Ensures safety for players, staff and supporters at every event.",
    image: images.squad,
  },
];

export type Match = {
  id: string;
  opponent: string;
  competition: string;
  date: string;
  time: string;
  stadium: string;
  home: boolean;
  scoreHome?: number;
  scoreAway?: number;
  status: "upcoming" | "played";
};

export const nextMatchDate = "2026-08-20T15:00:00";

export const matches: Match[] = [
  {
    id: "m1",
    opponent: "Kigali United",
    competition: "Premier League",
    date: "2026-08-20",
    time: "15:00",
    stadium: "Viking Stadium",
    home: true,
    status: "upcoming",
  },
  {
    id: "m2",
    opponent: "Sunrise FC",
    competition: "Premier League",
    date: "2026-08-27",
    time: "16:00",
    stadium: "Sunrise Arena",
    home: false,
    status: "upcoming",
  },
  {
    id: "m3",
    opponent: "Gasabo City",
    competition: "Cup",
    date: "2026-09-03",
    time: "15:30",
    stadium: "Viking Stadium",
    home: true,
    status: "upcoming",
  },
  {
    id: "m4",
    opponent: "Muhanga Stars",
    competition: "Premier League",
    date: "2026-09-10",
    time: "15:00",
    stadium: "Muhanga Ground",
    home: false,
    status: "upcoming",
  },
  {
    id: "r1",
    opponent: "Rayon Force",
    competition: "Premier League",
    date: "2026-07-28",
    time: "15:00",
    stadium: "Viking Stadium",
    home: true,
    scoreHome: 2,
    scoreAway: 1,
    status: "played",
  },
  {
    id: "r2",
    opponent: "Nyagatare FC",
    competition: "Premier League",
    date: "2026-07-21",
    time: "16:00",
    stadium: "Nyagatare Stadium",
    home: false,
    scoreHome: 0,
    scoreAway: 2,
    status: "played",
  },
  {
    id: "r3",
    opponent: "Bugesera SC",
    competition: "Cup",
    date: "2026-07-14",
    time: "15:00",
    stadium: "Viking Stadium",
    home: true,
    scoreHome: 3,
    scoreAway: 0,
    status: "played",
  },
  {
    id: "r4",
    opponent: "Musanze United",
    competition: "Premier League",
    date: "2026-07-07",
    time: "15:30",
    stadium: "Musanze Arena",
    home: false,
    scoreHome: 1,
    scoreAway: 1,
    status: "played",
  },
  {
    id: "r5",
    opponent: "Huye Titans",
    competition: "Premier League",
    date: "2026-06-30",
    time: "15:00",
    stadium: "Viking Stadium",
    home: true,
    scoreHome: 4,
    scoreAway: 2,
    status: "played",
  },
];

export type NewsArticle = {
  id: string;
  slug: string;
  title: string;
  summary: string;
  content: string;
  category:
    | "club"
    | "transfers"
    | "academy"
    | "matchReports"
    | "interviews"
    | "training"
    | "videos";
  image: string;
  date: string;
};

export const news: NewsArticle[] = [
  {
    id: "n1",
    slug: "vikings-ready-for-new-season",
    title: "Vikings Ready for a New Season of Glory",
    summary:
      "The squad completed pre-season preparations with intensity and unity at the training ground.",
    content:
      "Viking FC enters the new campaign with renewed ambition. After weeks of demanding sessions, Coach Michel Nzeyimana praised the squad's mentality and sharpness ahead of Match Day 1.",
    category: "club",
    image: images.celebrate,
    date: "2026-08-03",
  },
  {
    id: "n2",
    slug: "academy-stars-promoted",
    title: "Three Academy Stars Promoted to First Team",
    summary:
      "The club continues its commitment to youth by elevating homegrown talent.",
    content:
      "Three standout academy players have been promoted following impressive performances in youth competitions and training with the seniors.",
    category: "academy",
    image: images.stretch,
    date: "2026-08-01",
  },
  {
    id: "n3",
    slug: "match-report-rayon-force",
    title: "Match Report: Viking FC 2-1 Rayon Force",
    summary:
      "A late winner sealed three vital points in a hard-fought home victory.",
    content:
      "Viking FC showed character to come from behind and defeat Rayon Force. The decisive goal arrived in the 84th minute to the roar of the Viking faithful.",
    category: "matchReports",
    image: images.blueKit,
    date: "2026-07-28",
  },
  {
    id: "n4",
    slug: "new-signing-announcement",
    title: "Club Confirms New Midfield Signing",
    summary:
      "A creative midfielder joins Viking FC to strengthen the engine room.",
    content:
      "The Directeur Sportif confirmed a new midfield addition as the club builds depth for a long Premier League and Cup campaign.",
    category: "transfers",
    image: images.jog,
    date: "2026-07-25",
  },
  {
    id: "n5",
    slug: "manager-interview",
    title: "Exclusive Interview with Coach Michel",
    summary:
      "Our manager discusses tactics, culture and the Viking dream.",
    content:
      "In an exclusive sit-down, Coach Michel shared his football philosophy and why Viking FC is ready to compete at the highest level.",
    category: "interviews",
    image: images.portrait,
    date: "2026-07-20",
  },
  {
    id: "n6",
    slug: "training-week-intensity",
    title: "Inside Training: High Intensity Week",
    summary:
      "Camera access to the Viking training pitch during a key preparation week.",
    content:
      "From warm-ups to tactical boards, this week's sessions showed a squad locked in and ready for battle.",
    category: "training",
    image: images.run,
    date: "2026-07-18",
  },
];

export const leagueTable = [
  { pos: 1, team: "Viking FC", played: 10, won: 7, drawn: 2, lost: 1, gf: 21, ga: 8, gd: 13, pts: 23 },
  { pos: 2, team: "Rayon Force", played: 10, won: 6, drawn: 3, lost: 1, gf: 18, ga: 9, gd: 9, pts: 21 },
  { pos: 3, team: "Kigali United", played: 10, won: 6, drawn: 1, lost: 3, gf: 16, ga: 11, gd: 5, pts: 19 },
  { pos: 4, team: "Sunrise FC", played: 10, won: 5, drawn: 3, lost: 2, gf: 14, ga: 10, gd: 4, pts: 18 },
  { pos: 5, team: "Gasabo City", played: 10, won: 4, drawn: 3, lost: 3, gf: 12, ga: 12, gd: 0, pts: 15 },
  { pos: 6, team: "Musanze United", played: 10, won: 3, drawn: 4, lost: 3, gf: 11, ga: 12, gd: -1, pts: 13 },
  { pos: 7, team: "Huye Titans", played: 10, won: 3, drawn: 2, lost: 5, gf: 10, ga: 15, gd: -5, pts: 11 },
  { pos: 8, team: "Muhanga Stars", played: 10, won: 2, drawn: 2, lost: 6, gf: 8, ga: 16, gd: -8, pts: 8 },
];

export const sponsors = [
  { name: "Madeleine Group", logo: images.logo },
  { name: "Nordic Energy", logo: images.logo },
  { name: "Kigali Bank", logo: images.logo },
  { name: "Arena Sportswear", logo: images.logo },
  { name: "Horizon Telecom", logo: images.logo },
];

export const shopProducts = [
  { id: "p1", name: "Home Jersey 2026", category: "jerseys", price: 45000, image: images.blueKit },
  { id: "p2", name: "Away Jersey 2026", category: "jerseys", price: 45000, image: images.whiteKit },
  { id: "p3", name: "Training Jersey", category: "jerseys", price: 35000, image: images.pitch },
  { id: "p4", name: "Viking Cap", category: "caps", price: 15000, image: images.logo },
  { id: "p5", name: "Club Scarf", category: "scarves", price: 18000, image: images.logo },
  { id: "p6", name: "Match Boots", category: "boots", price: 85000, image: images.run },
  { id: "p7", name: "Gym Bag", category: "accessories", price: 28000, image: images.stretch },
  { id: "p8", name: "Wristband Set", category: "accessories", price: 8000, image: images.celebrate },
];

export const galleryItems = [
  { id: "g1", type: "photos", title: "Squad Portrait", image: images.portrait, category: "moments" },
  { id: "g2", type: "photos", title: "Blue Kit Team", image: images.blueKit, category: "moments" },
  { id: "g3", type: "photos", title: "White Kit Squad", image: images.whiteKit, category: "moments" },
  { id: "g4", type: "photos", title: "Full Squad", image: images.squad, category: "moments" },
  { id: "g5", type: "photos", title: "Studio Session", image: images.studio, category: "moments" },
  { id: "g6", type: "photos", title: "Training Run", image: images.run, category: "training" },
  { id: "g7", type: "photos", title: "Warm-up Jog", image: images.jog, category: "training" },
  { id: "g8", type: "photos", title: "Stretch Session", image: images.stretch, category: "training" },
  { id: "g9", type: "photos", title: "Pitch Session", image: images.pitch, category: "training" },
  { id: "g10", type: "photos", title: "Celebration", image: images.celebrate, category: "fans" },
  { id: "g11", type: "videos", title: "Club Highlight Reel", image: images.celebrate, category: "videos" },
  { id: "g12", type: "photos", title: "Champions Spirit", image: images.logo, category: "trophies" },
];

export const services = [
  { id: "tickets", titleKey: "tickets" as const, desc: "Secure your seat for every Viking home match.", image: images.blueKit },
  { id: "academy", titleKey: "academy" as const, desc: "Develop the next generation of Viking talent.", image: images.stretch },
  { id: "recruitment", titleKey: "recruitment" as const, desc: "Join the Viking pathway as a player or coach.", image: images.jog },
  { id: "partnership", titleKey: "partnership" as const, desc: "Grow your brand with an ambitious football club.", image: images.studio },
  { id: "stadiumRental", titleKey: "stadiumRental" as const, desc: "Host events at Viking Stadium facilities.", image: images.squad },
  { id: "coaching", titleKey: "coaching" as const, desc: "Professional coaching clinics and camps.", image: images.pitch },
  { id: "boutique", titleKey: "boutique" as const, desc: "Official merchandise for true supporters.", image: images.whiteKit },
];

/* Viking FC — Club Data */
const CLUB = {
  name: "VIKING FC",
  slogan: "One Club. One Family. One Dream.",
  logo: "logo/viking-fc.png",
  email: "vikingfc.official@gmail.com",
  phone: "+250 788 123 456",
  address: "Ejo Heza, Kigali, Rwanda",
  president: {
    name: "Victor Jacker",
    email: "president.vikingfc@gmail.com",
    phone: "+250 788 654 321",
  },
  social: {
    instagram:
      "https://www.instagram.com/viking_football.club?igsh=MXIwNTM4MWV0d211ag%3D%3D&utm_source=qr",
    tiktok: "https://www.tiktok.com/@fc.vikings1",
    facebook: "https://facebook.com/vikingfc",
    youtube: "https://youtube.com/@vikingfc",
    whatsapp: "https://wa.me/250788123456",
  },
};

const IMAGES = {
  studio: "images/studio.png",
  whiteKit: "images/white-kit.png",
  blueKit: "images/blue-kit.png",
  squad: "images/squad.png",
  portrait: "images/portrait.png",
  pitch: "images/pitch.png",
  jog: "images/jog.png",
  run: "images/run.png",
  stretch: "images/stretch.png",
  celebrate: "images/celebrate.png",
  video: "videos/hero.mp4",
  club: {
    championsSquad: "images/club/champions-squad.jpg",
    coachBest: "images/club/coach-best-trophies.jpg",
    portraitCap: "images/club/portrait-cap-glasses.jpg",
    portraitPolo: "images/club/portrait-white-polo.jpg",
    winnersCrouch: "images/club/winners-crouch-trophy.jpg",
    winnersFive: "images/club/winners-five-line.jpg",
    winnersFiveAlt: "images/club/winners-five-alt.jpg",
    winnersFour: "images/club/winners-four-caf.jpg",
    winnersThreeDrc: "images/club/winners-three-drc-flag.jpg",
    winnersThreeGrey: "images/club/winners-three-grey-cap.jpg",
    winnersThreePrada: "images/club/winners-three-prada.jpg",
    groupCelebration: "images/club/winners-group-celebration.jpg",
    groupDense: "images/club/winners-group-dense.jpg",
    groupEnergy: "images/club/winners-group-energy.jpg",
    groupFlags: "images/club/winners-group-flags.jpg",
    groupMedals: "images/club/winners-group-medals.jpg",
    groupTrophies: "images/club/winners-group-trophies.jpg",
    uzaFormal: "images/club/uza-three-formal.jpg",
    uzaCasual: "images/club/uza-three-casual.jpg",
  },
  staff: {
    victorJacker: "images/staff/victor-jacker.jpg",
    josephCourbon: "images/staff/joseph-courbon.jpg",
    ardentMatabaro: "images/staff/ardent-matabaro.jpg",
    rolande: "images/staff/rolande.jpg",
    rodrigueShabani: "images/staff/rodrigue-shabani.jpg",
  },
};

const VIDEOS = {
  hero: "videos/hero.mp4",
  highlight1: "videos/highlight-1.mp4",
  highlight2: "videos/highlight-2.mp4",
};

const NEXT_MATCH_DATE = "2026-08-16T14:00:00";

const PLAYERS = [
  { id: "1", slug: "eric-nshimiyimana", name: "Eric Nshimiyimana", number: 1, position: "Goalkeeper", nationality: "Rwanda", age: 24, height: "188 cm", weight: "82 kg", dob: "2002-03-14", image: IMAGES.portrait, bio: "Commanding shot-stopper and vocal leader from the back.", stats: { appearances: 28, goals: 0, assists: 1, cleanSheets: 12 } },
  { id: "2", slug: "jean-baptiste-uwase", name: "Jean Baptiste Uwase", number: 4, position: "Defender", nationality: "Rwanda", age: 26, height: "185 cm", weight: "78 kg", dob: "2000-07-22", image: IMAGES.pitch, bio: "Reliable centre-back with strong aerial ability.", stats: { appearances: 30, goals: 2, assists: 1 } },
  { id: "3", slug: "david-habimana", name: "David Habimana", number: 5, position: "Defender", nationality: "Rwanda", age: 23, height: "182 cm", weight: "76 kg", dob: "2003-01-09", image: IMAGES.squad, bio: "Modern full-back who loves to push forward.", stats: { appearances: 27, goals: 1, assists: 5 } },
  { id: "4", slug: "patrick-mugisha", name: "Patrick Mugisha", number: 3, position: "Defender", nationality: "Rwanda", age: 25, height: "180 cm", weight: "74 kg", dob: "2001-11-02", image: IMAGES.whiteKit, bio: "Tough left-back with excellent recovery pace.", stats: { appearances: 25, goals: 0, assists: 3 } },
  { id: "5", slug: "alex-iradukunda", name: "Alex Iradukunda", number: 6, position: "Midfielder", nationality: "Rwanda", age: 27, height: "178 cm", weight: "73 kg", dob: "1999-05-18", image: IMAGES.jog, bio: "Engine of the midfield — tireless and tactically sharp.", stats: { appearances: 31, goals: 3, assists: 7 } },
  { id: "6", slug: "kevin-nkurunziza", name: "Kevin Nkurunziza", number: 8, position: "Midfielder", nationality: "Rwanda", age: 22, height: "176 cm", weight: "70 kg", dob: "2004-08-30", image: IMAGES.run, bio: "Creative midfielder with a sharp final pass.", stats: { appearances: 29, goals: 6, assists: 11 } },
  { id: "7", slug: "samuel-hakizimana", name: "Samuel Hakizimana", number: 10, position: "Midfielder", nationality: "Rwanda", age: 24, height: "175 cm", weight: "71 kg", dob: "2002-12-05", image: IMAGES.celebrate, bio: "Club playmaker and set-piece specialist.", stats: { appearances: 30, goals: 9, assists: 14 } },
  { id: "8", slug: "christian-mbonyumuremyi", name: "Christian Mbonyumuremyi", number: 7, position: "Forward", nationality: "Rwanda", age: 23, height: "179 cm", weight: "74 kg", dob: "2003-04-21", image: IMAGES.blueKit, bio: "Explosive winger with deadly pace on the break.", stats: { appearances: 28, goals: 12, assists: 8 } },
  { id: "9", slug: "felix-tuyisenge", name: "Felix Tuyisenge", number: 9, position: "Forward", nationality: "Rwanda", age: 25, height: "184 cm", weight: "80 kg", dob: "2001-09-12", image: IMAGES.stretch, bio: "Clinical striker and focal point of the attack.", stats: { appearances: 30, goals: 18, assists: 4 } },
  { id: "10", slug: "olive-manzi", name: "Olive Manzi", number: 11, position: "Forward", nationality: "Rwanda", age: 21, height: "177 cm", weight: "72 kg", dob: "2005-02-28", image: IMAGES.studio, bio: "Young talent with fearless dribbling and finishing.", stats: { appearances: 22, goals: 8, assists: 6 } },
  { id: "11", slug: "bruno-gasana", name: "Bruno Gasana", number: 17, position: "Midfielder", nationality: "Rwanda", age: 20, height: "174 cm", weight: "69 kg", dob: "2006-06-15", image: IMAGES.pitch, bio: "Academy graduate rising fast into the first team.", stats: { appearances: 15, goals: 2, assists: 3 } },
  { id: "12", slug: "yves-rutayisire", name: "Yves Rutayisire", number: 19, position: "Defender", nationality: "Rwanda", age: 22, height: "183 cm", weight: "77 kg", dob: "2004-10-03", image: IMAGES.squad, bio: "Composed defender with excellent reading of the game.", stats: { appearances: 20, goals: 1, assists: 0 } },
];

const STAFF_SECTIONS = [
  {
    id: "administration",
    title: "Administration",
    subtitle: "Club leadership & management",
    members: [
      {
        name: "Victor Jacker",
        role: "Club President",
        bio: "Leads Viking FC vision, governance and institutional growth.",
        image: IMAGES.staff.victorJacker,
      },
      {
        name: "Joseph Courbon",
        role: "General Manager / CEO",
        bio: "Responsible for day-to-day club operations and strategic direction.",
        image: IMAGES.staff.josephCourbon,
      },
      {
        name: "Ardent Matabaro",
        role: "Financial Director",
        bio: "Oversees club finances, budgeting and long-term financial planning.",
        image: IMAGES.staff.ardentMatabaro,
      },
      {
        name: "Rodrigue Shabani",
        role: "IT Director",
        bio: "Leads club technology, digital systems and IT infrastructure.",
        image: IMAGES.staff.rodrigueShabani,
      },
    ],
  },
  {
    id: "technical",
    title: "Technical Staff",
    subtitle: "First team coaching staff",
    members: [
      {
        name: "Coach Michel Nzeyimana",
        role: "First Team Head Coach",
        bio: "Head coach of the first team. High-pressing football and youth development.",
        image: IMAGES.pitch,
      },
      {
        name: "Thierry Niyonshuti",
        role: "Assistant Coach",
        bio: "Supports match preparation, tactics and player development.",
        image: IMAGES.jog,
      },
      {
        name: "Samuel Bizimana",
        role: "Goalkeeper Coach",
        bio: "Specialist coach for shot-stopping, distribution and GK leadership.",
        image: IMAGES.blueKit,
      },
      {
        name: "Dr. Diane Mukamana",
        role: "Fitness Coach",
        bio: "Elite conditioning programs for peak match performance.",
        image: IMAGES.stretch,
      },
      {
        name: "Jean Paul Habimana",
        role: "Team Manager",
        bio: "Coordinates team logistics, travel and matchday organisation.",
        image: IMAGES.squad,
      },
    ],
  },
  {
    id: "medical",
    title: "Medical Staff",
    subtitle: "Player health & recovery",
    members: [
      {
        name: "Rolande",
        role: "Club Doctor",
        bio: "Club doctor responsible for player health, medical readiness and wellbeing.",
        image: IMAGES.staff.rolande,
      },
      {
        name: "Grace Ingabire",
        role: "Physiotherapist",
        bio: "Injury prevention, recovery and rehabilitation specialist.",
        image: IMAGES.celebrate,
      },
    ],
  },
];

/** Flat list kept for admin / compatibility */
const STAFF = STAFF_SECTIONS.flatMap((section) =>
  section.members.map((m) => ({ ...m, category: section.title })),
);

const MATCHES = [
  // Résultats connus — calendrier retour / aller GSAL 2026
  {
    id: "j8",
    opponent: "Black Star",
    competition: "GSAL 2026",
    matchday: "J8",
    date: "2026-08-08",
    time: "14:00",
    stadium: "Ejo Heza",
    home: false,
    status: "played",
  },
  // Matchs à venir — calendrier officiel
  {
    id: "j9",
    opponent: "Mapane FC",
    competition: "GSAL 2026",
    matchday: "J9",
    date: "2026-08-16",
    time: "14:00",
    stadium: "Ejo Heza",
    home: false,
    status: "upcoming",
  },
  {
    id: "j10",
    opponent: "La Seleção",
    competition: "GSAL 2026",
    matchday: "J10",
    date: "2026-08-23",
    time: "16:00",
    stadium: "Ejo Heza",
    home: false,
    status: "upcoming",
  },
  {
    id: "j11",
    opponent: "AS Training",
    competition: "GSAL 2026",
    matchday: "J11",
    date: "2026-08-29",
    time: "14:00",
    stadium: "Ejo Heza",
    home: false,
    status: "upcoming",
  },
  {
    id: "j12",
    opponent: "RCK",
    competition: "GSAL 2026",
    matchday: "J12",
    date: "2026-09-05",
    time: "16:00",
    stadium: "Ejo Heza",
    home: true,
    status: "upcoming",
  },
  {
    id: "j13",
    opponent: "ASPK",
    competition: "GSAL 2026",
    matchday: "J13",
    date: "2026-09-13",
    time: "16:00",
    stadium: "Ejo Heza",
    home: true,
    status: "upcoming",
  },
  {
    id: "j14",
    opponent: "AS Black Panthers",
    competition: "GSAL 2026",
    matchday: "J14",
    date: "2026-09-19",
    time: "14:00",
    stadium: "Ejo Heza",
    home: true,
    status: "upcoming",
  },
];

const NEWS = [
  { id: "n1", slug: "vikings-ready-for-new-season", title: "Vikings Ready for a New Season of Glory", summary: "The squad completed pre-season preparations with intensity and unity at the training ground.", content: "Viking FC enters the new campaign with renewed ambition. After weeks of demanding sessions, Coach Michel Nzeyimana praised the squad's mentality and sharpness ahead of Match Day 1.", category: "club", image: IMAGES.celebrate, date: "2026-08-03" },
  { id: "n2", slug: "academy-stars-promoted", title: "Three Academy Stars Promoted to First Team", summary: "The club continues its commitment to youth by elevating homegrown talent.", content: "Three standout academy players have been promoted following impressive performances in youth competitions and training with the seniors.", category: "academy", image: IMAGES.stretch, date: "2026-08-01" },
  { id: "n3", slug: "match-report-rayon-force", title: "Match Report: Viking FC 2-1 Rayon Force", summary: "A late winner sealed three vital points in a hard-fought home victory.", content: "Viking FC showed character to come from behind and defeat Rayon Force. The decisive goal arrived in the 84th minute to the roar of the Viking faithful.", category: "matchReports", image: IMAGES.blueKit, date: "2026-07-28" },
  { id: "n4", slug: "new-signing-announcement", title: "Club Confirms New Midfield Signing", summary: "A creative midfielder joins Viking FC to strengthen the engine room.", content: "The Directeur Sportif confirmed a new midfield addition as the club builds depth for a long Premier League and Cup campaign.", category: "transfers", image: IMAGES.jog, date: "2026-07-25" },
  { id: "n5", slug: "manager-interview", title: "Exclusive Interview with Coach Michel", summary: "Our manager discusses tactics, culture and the Viking dream.", content: "In an exclusive sit-down, Coach Michel shared his football philosophy and why Viking FC is ready to compete at the highest level.", category: "interviews", image: IMAGES.portrait, date: "2026-07-20" },
  { id: "n6", slug: "training-week-intensity", title: "Inside Training: High Intensity Week", summary: "Camera access to the Viking training pitch during a key preparation week.", content: "From warm-ups to tactical boards, this week's sessions showed a squad locked in and ready for battle.", category: "training", image: IMAGES.run, date: "2026-07-18" },
];

const LEAGUE_TABLE = [
  { pos: 1, team: "Viking FC", played: 8, won: 8, drawn: 0, lost: 0, gf: 22, ga: 4, gd: 18, pts: 24 },
  { pos: 2, team: "ASPK", played: 8, won: 6, drawn: 1, lost: 1, gf: 24, ga: 6, gd: 18, pts: 19 },
  { pos: 3, team: "RCK", played: 8, won: 5, drawn: 1, lost: 2, gf: 23, ga: 11, gd: 12, pts: 16 },
  { pos: 4, team: "Black Panthers", played: 8, won: 5, drawn: 0, lost: 3, gf: 16, ga: 11, gd: 5, pts: 15 },
  { pos: 5, team: "Black Star", played: 8, won: 1, drawn: 3, lost: 4, gf: 11, ga: 18, gd: -7, pts: 6 },
  { pos: 6, team: "Mapane FC", played: 8, won: 1, drawn: 3, lost: 4, gf: 9, ga: 16, gd: -7, pts: 6 },
  { pos: 7, team: "AS Training", played: 8, won: 1, drawn: 2, lost: 5, gf: 6, ga: 16, gd: -10, pts: 5 },
  { pos: 8, team: "La Seleção", played: 8, won: 0, drawn: 0, lost: 8, gf: 9, ga: 38, gd: -29, pts: 0 },
];

const LEAGUE_META = {
  name: "GSAL 2026",
  label: "Classement provisoire après J8",
  labelEn: "Provisional standings after MD8",
  venue: "Ejo Heza",
};

const SPONSORS = ["Gitera Safari", "Madeleine Group"];

const SHOP = [
  { id: "p1", name: "Home Jersey 2026", category: "jerseys", price: 45000, image: IMAGES.blueKit },
  { id: "p2", name: "Away Jersey 2026", category: "jerseys", price: 45000, image: IMAGES.whiteKit },
  { id: "p3", name: "Training Jersey", category: "jerseys", price: 35000, image: IMAGES.pitch },
  { id: "p4", name: "Viking Cap", category: "caps", price: 15000, image: CLUB.logo },
  { id: "p5", name: "Club Scarf", category: "scarves", price: 18000, image: CLUB.logo },
  { id: "p6", name: "Match Boots", category: "boots", price: 85000, image: IMAGES.run },
  { id: "p7", name: "Gym Bag", category: "accessories", price: 28000, image: IMAGES.stretch },
  { id: "p8", name: "Wristband Set", category: "accessories", price: 8000, image: IMAGES.celebrate },
];

const GALLERY = [
  { title: "Squad Portrait", image: IMAGES.portrait, type: "photos", category: "moments" },
  { title: "Blue Kit Team", image: IMAGES.blueKit, type: "photos", category: "moments" },
  { title: "White Kit Squad", image: IMAGES.whiteKit, type: "photos", category: "moments" },
  { title: "Full Squad", image: IMAGES.squad, type: "photos", category: "moments" },
  { title: "Studio Session", image: IMAGES.studio, type: "photos", category: "moments" },
  { title: "Training Run", image: IMAGES.run, type: "photos", category: "training" },
  { title: "Warm-up Jog", image: IMAGES.jog, type: "photos", category: "training" },
  { title: "Stretch Session", image: IMAGES.stretch, type: "photos", category: "training" },
  { title: "Pitch Session", image: IMAGES.pitch, type: "photos", category: "training" },
  { title: "Celebration", image: IMAGES.celebrate, type: "photos", category: "fans" },
  { title: "Champions Night", image: IMAGES.club.championsSquad, type: "photos", category: "trophies" },
  { title: "Winners Trophy", image: IMAGES.club.groupTrophies, type: "photos", category: "trophies" },
  {
    title: "Matchday Highlight 1",
    image: IMAGES.club.championsSquad,
    video: VIDEOS.highlight1,
    type: "videos",
    category: "videos",
  },
  {
    title: "Matchday Highlight 2",
    image: IMAGES.club.groupCelebration,
    video: VIDEOS.highlight2,
    type: "videos",
    category: "videos",
  },
];

/** Fix asset paths when page is inside /pages/ */
function asset(path) {
  const inPages = window.location.pathname.replace(/\\/g, "/").includes("/pages/");
  if (!path) return path;
  if (path.startsWith("http") || path.startsWith("/") || path.startsWith("../")) return path;
  return inPages ? "../" + path : path;
}

function pageLink(file) {
  const inPages = window.location.pathname.replace(/\\/g, "/").includes("/pages/");
  if (file === "index.html" || file === "") {
    return inPages ? "../index.html" : "index.html";
  }
  return inPages ? file : "pages/" + file;
}

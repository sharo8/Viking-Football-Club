import { L, type LocalizedString } from "@/lib/localize";

export const club = {
  name: "VIKING FC",
  slogan: "One Club. One Family. One Dream.",
  logo: "/logo/viking-fc.png",
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
  logo: "/logo/viking-fc.png",
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
  coachJocos: "/images/coach-jocos-courbon.png",
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
  matchday?: string;
  date: string;
  time: string;
  stadium: string;
  home: boolean;
  scoreHome?: number;
  scoreAway?: number;
  status: "upcoming" | "played";
};

export const nextMatchDate = "2026-08-23T16:00:00";

export const matches: Match[] = [
  {
    id: "j8",
    opponent: "Black Star de Kigali",
    competition: "GSAL-League",
    matchday: "J8",
    date: "2026-08-08",
    time: "14:00",
    stadium: "Ejo Heza Stadium",
    home: true,
    scoreHome: 5,
    scoreAway: 0,
    status: "played",
  },
  {
    id: "aspk",
    opponent: "ASPK",
    competition: "GSAL-League",
    date: "2026-08-02",
    time: "15:00",
    stadium: "Ejo Heza Stadium",
    home: true,
    scoreHome: 2,
    scoreAway: 0,
    status: "played",
  },
  {
    id: "panthers",
    opponent: "AS Black Panthers",
    competition: "GSAL-League",
    date: "2026-07-26",
    time: "15:00",
    stadium: "Ejo Heza Stadium",
    home: true,
    scoreHome: 1,
    scoreAway: 0,
    status: "played",
  },
  {
    id: "mapane",
    opponent: "Mapane FC",
    competition: "GSAL-League",
    date: "2026-07-19",
    time: "15:00",
    stadium: "Ejo Heza Stadium",
    home: true,
    scoreHome: 3,
    scoreAway: 0,
    status: "played",
  },
  {
    id: "selecao",
    opponent: "La Seleção",
    competition: "GSAL-League",
    date: "2026-07-12",
    time: "15:00",
    stadium: "Ejo Heza Stadium",
    home: true,
    scoreHome: 2,
    scoreAway: 0,
    status: "played",
  },
  {
    id: "next1",
    opponent: "RCK",
    competition: "GSAL-League",
    matchday: "J11",
    date: "2026-08-23",
    time: "16:00",
    stadium: "Ejo Heza Stadium",
    home: true,
    status: "upcoming",
  },
  {
    id: "next2",
    opponent: "AS Training",
    competition: "GSAL-League",
    matchday: "J12",
    date: "2026-08-30",
    time: "14:00",
    stadium: "Ejo Heza Stadium",
    home: false,
    status: "upcoming",
  },
  {
    id: "next3",
    opponent: "ASPK",
    competition: "GSAL-League",
    matchday: "J13",
    date: "2026-09-06",
    time: "16:00",
    stadium: "Ejo Heza Stadium",
    home: true,
    status: "upcoming",
  },
];

export type NewsArticle = {
  id: string;
  slug: string;
  title: LocalizedString;
  summary: LocalizedString;
  content: LocalizedString;
  category:
    | "club"
    | "transfers"
    | "academy"
    | "matchReports"
    | "interviews"
    | "training"
    | "videos"
    | "retirement";
  image: string;
  date: string;
};

export const news: NewsArticle[] = [
  {
    id: "n-season-ready",
    slug: "viking-fc-ready-for-the-new-season",
    title: L(
      "Viking FC Ready For The New Season",
      "Viking FC prêt pour la nouvelle saison",
      "Viking FC yiteguye igihe gishya",
    ),
    summary: L(
      "Viking FC starts a new season with ambition and determination. After intensive preparation, the team is ready to defend its colours and pursue excellence in the GSAL-League.",
      "Viking FC entame une nouvelle saison avec ambition et détermination. Après une préparation intensive, l'équipe est prête à défendre ses couleurs et à poursuivre sa quête d'excellence dans la GSAL-League.",
      "Viking FC itangira igihe gishya n'intego n'ubushake. Nyuma y'imyitozo ikomeye, ikipe yiteguye kurengera ibara ryayo no gukomeza gutsinda muri GSAL-League.",
    ),
    content: L(
      "Viking FC starts a new season with ambition and determination. After intensive preparation, the team is ready to defend its colours and pursue excellence in the GSAL-League. Staff and players approach this campaign with the standards and family spirit that define the club.",
      "Viking FC entame une nouvelle saison avec ambition et détermination. Après une préparation intensive, l'équipe est prête à défendre ses couleurs et à poursuivre sa quête d'excellence dans la GSAL-League. Le staff et le vestiaire abordent cette campagne avec l'exigence et l'esprit de famille qui définissent le club.",
      "Viking FC itangira igihe gishya n'intego n'ubushake. Nyuma y'imyitozo ikomeye, ikipe yiteguye kurengera ibara ryayo no gukomeza gutsinda muri GSAL-League. Abatoza n'abakinnyi bahura n'iyi kampanyi bafite umuco n'umuryango bigaragaza ikipe.",
    ),
    category: "club",
    image: images.squad,
    date: "2026-08-20",
  },
  {
    id: "n-jacker-sharo",
    slug: "retraite-victor-jacker-et-sharo",
    title: L(
      "End of an Era: Victor Jacker and Sharo Retire",
      "Fin d'une Ère : Départ à la Retraite de Victor Jacker et Sharo",
      "Iherezo ry'igihe: Victor Jacker na Sharo basubira mu kiruhuko",
    ),
    summary: L(
      "With deep emotion, Viking FC announces the retirement of two club legends. Victor Jacker and Sharo step away after more than two years of loyal service.",
      "C'est avec une profonde tristesse que Viking FC annonce le départ à la retraite de deux légendes du club. Victor Jacker et Sharo tirent leur révérence après plus de deux ans de loyaux services.",
      "Viking FC yamenyesheje n'agahinda ko Victor Jacker na Sharo basubiye mu kiruhuko. Batangiye nyuma y'imyaka irenga ibiri bakorera ikipe n'ubudahemuka.",
    ),
    content: L(
      "With deep emotion, Viking FC announces the retirement of two club legends. Victor Jacker, captain and all-time assist leader of the amateur era with more than 15 assists, and Sharo, vice-captain and top scorer with more than 30 goals in Viking colours, step away after more than two years of loyal service. Together they led the team to three consecutive trophies, giving everything for the club. Jacker's number 11 and Sharo's number 7 will remain forever in Viking FC history. They arrived as kings and leave as legends.",
      "C'est avec une profonde tristesse que Viking FC annonce le départ à la retraite de deux légendes du club. Victor Jacker, capitaine et meilleur passeur de l'histoire du club amateur avec plus de 15 passes décisives, ainsi que Sharo, sous-capitaine et meilleur buteur avec plus de 30 buts sous le maillot Viking, tirent leur révérence après plus de deux ans de loyaux services. Ensemble, ils ont porté l'équipe vers trois trophées consécutifs, jouant chaque match avec passion et en donnant tout pour le club. Le numéro 11 de Victor Jacker et le numéro 7 de Sharo resteront gravés à jamais dans l'histoire de Viking FC. Arrivés comme des rois, ils repartent comme des légendes.",
      "Viking FC yamenyesheje n'agahinda ko Victor Jacker na Sharo basubiye mu kiruhuko. Victor Jacker, kapiteni n'uwatanga amahita menshi mu mateka y'ikipe y'abanyamateka n'amahita arenga 15, na Sharo, umukuru wungirije n'uwatsinze ibitego byinshi birenga 30 mu kanzu ya Viking, batangira nyuma y'imyaka irenga ibiri. Hamwe bayoboye ikipe ku bihembo bitatu ukurikiranya. Nimero 11 ya Jacker na 7 ya Sharo bizahora mu mateka ya Viking FC. Baje nk'abami, basubuka nk'imitego.",
    ),
    category: "retirement",
    image: images.celebrate,
    date: "2026-08-18",
  },
  {
    id: "n-jocos",
    slug: "retraite-coach-joseph-jocos-courbon",
    title: L(
      'Retirement of Coach Courbon "Jocos Ancelotti"',
      'Départ à la Retraite du Coach Courbon "Jocos Ancelotti"',
      'Gusubira mu kiruhuko kwa Coach Courbon "Jocos Ancelotti"',
    ),
    summary: L(
      'With great emotion, Viking FC announces the retirement of Coach Courbon "Jocos Ancelotti" after more than two years at the head of the technical staff.',
      'C\'est avec une grande émotion que Viking FC annonce le départ à la retraite de Coach Courbon "Jocos Ancelotti", après plus de deux ans à la tête de l\'équipe technique.',
      'Viking FC yamenyesheje n\'umutima ko Coach Courbon "Jocos Ancelotti" asubira mu kiruhuko, nyuma y\'imyaka irenga ibiri ayoboye abatoza.',
    ),
    content: L(
      'With great emotion, Viking FC announces the retirement of Coach Courbon "Jocos Ancelotti" after more than two years at the head of the technical staff. A true pioneer of Viking football, he led the club to three trophies during his tenure while developing players with high standards and care. A living legend of the club, he will be remembered as the architect of this glorious era. He arrived as a prince and leaves as a king of Kategate. Thank you coach, you will be missed.',
      'C\'est avec une grande émotion que Viking FC annonce le départ à la retraite de Coach Courbon "Jocos Ancelotti", après plus de deux ans à la tête de l\'équipe technique. Véritable révolutionnaire du football viking, il a conduit le club à trois trophées durant son mandat, tout en formant et en accompagnant les joueurs avec exigence et bienveillance. Légende vivante du club, il restera dans les mémoires comme l\'architecte de cette ère glorieuse. Arrivé comme un prince, il repart comme un roi de Kategate. Merci coach, tu vas nous manquer.',
      'Viking FC yamenyesheje n\'umutima ko Coach Courbon "Jocos Ancelotti" asubira mu kiruhuko, nyuma y\'imyaka irenga ibiri ayoboye abatoza. Ni umuntu wahinduye umupira w\'amaguru wa Viking, ayoboye ikipe ku bihembo bitatu mu gihe cye, akomeza gutanga imyitozo n\'uburyo bwiza ku bakinnyi. Azahora mu mateka nk\'uwubatsye iki gihe cy\'icyubahiro. Yaje nk\'umwami muto, asubuka nk\'umwami wa Kategate. Urakoze coach, tuzakwibuka.',
    ),
    category: "retirement",
    image: images.coachJocos,
    date: "2026-08-16",
  },
  // TODO: replace with real content
  {
    id: "n-placeholder-rck-preview",
    slug: "preview-viking-fc-vs-rck",
    title: L(
      "Match Preview: Viking FC Host RCK Under Pressure",
      "Avant-match : Viking FC reçoit RCK sous pression",
      "Ibanze ry'umukino: Viking FC yakiriye RCK",
    ),
    summary: L(
      "Placeholder — Leaders Viking FC prepare to host RCK in a pivotal GSAL-League clash at Ejo Heza Stadium. The coaching staff have intensified tactical sessions as the club aim to keep their perfect start alive.",
      "Placeholder — Leaders Viking FC se préparent à recevoir RCK dans un choc décisif de GSAL-League à Ejo Heza. Le staff a intensifié les séances tactiques pour préserver le départ parfait du club.",
      "Placeholder — Abayobozi ba Viking FC biteguye kwakira RCK mu mukino w'ingenzi wa GSAL-League kuri Ejo Heza. Abatoza bakomeje imyitozo kugira ngo bakomeze intsinzi.",
    ),
    content: L(
      "Placeholder — Leaders Viking FC prepare to host RCK in a pivotal GSAL-League clash at Ejo Heza Stadium. The coaching staff have intensified tactical sessions as the club aim to keep their perfect start alive. Fans are expected in numbers for what could be one of the biggest home nights of the campaign so far.",
      "Placeholder — Leaders Viking FC se préparent à recevoir RCK dans un choc décisif de GSAL-League à Ejo Heza. Le staff a intensifié les séances tactiques pour préserver le départ parfait du club. Les supporters sont attendus en nombre pour l'une des plus grandes soirées à domicile de la saison.",
      "Placeholder — Abayobozi ba Viking FC biteguye kwakira RCK mu mukino w'ingenzi wa GSAL-League kuri Ejo Heza. Abatoza bakomeje imyitozo kugira ngo bakomeze intsinzi. Abafana biteganyijwe ku bwinshi muri uyu mukino ukomeye.",
    ),
    category: "matchReports",
    image: images.pitch,
    date: "2026-08-15",
  },
  // TODO: replace with real content
  {
    id: "n-placeholder-partner-day",
    slug: "club-partners-community-day",
    title: L(
      "Partners & Community Day at Ejo Heza",
      "Journée partenaires & communauté à Ejo Heza",
      "Umunsi w'abafatanyabikorwa n'umuryango kuri Ejo Heza",
    ),
    summary: L(
      "Placeholder — Viking FC and club partners Gitera Safari and Madeleine Group open the stadium gates for a community afternoon. Supporters, academy families and local guests will meet players ahead of the GSAL-League run-in.",
      "Placeholder — Viking FC et les partenaires Gitera Safari et Madeleine Group ouvrent les portes du stade pour un après-midi communautaire. Supporters, familles de l'académie et invités locaux rencontreront les joueurs.",
      "Placeholder — Viking FC n'abafatanyabikorwa Gitera Safari na Madeleine Group bafunguye sitade ku manywa y'umuryango. Abafana, imiryango y'akademi n'abashyitsi bahura n'abakinnyi.",
    ),
    content: L(
      "Placeholder — Viking FC and club partners Gitera Safari and Madeleine Group open the stadium gates for a community afternoon. Supporters, academy families and local guests will meet players ahead of the GSAL-League run-in. The event underlines the club’s family spirit beyond matchdays.",
      "Placeholder — Viking FC et les partenaires Gitera Safari et Madeleine Group ouvrent les portes du stade pour un après-midi communautaire. Supporters, familles de l'académie et invités locaux rencontreront les joueurs. L'événement rappelle l'esprit de famille du club au-delà des matchs.",
      "Placeholder — Viking FC n'abafatanyabikorwa Gitera Safari na Madeleine Group bafunguye sitade ku manywa y'umuryango. Abafana, imiryango y'akademi n'abashyitsi bahura n'abakinnyi. Iki gikorwa kigaragara umuryango w'ikipe hanze y'imikino.",
    ),
    category: "club",
    image: images.studio,
    date: "2026-08-14",
  },
  // TODO: replace with real content
  {
    id: "n-placeholder-academy-prospect",
    slug: "academy-prospect-earns-senior-look",
    title: L(
      "Academy Prospect Earns Senior Training Look",
      "Un prospect de l'académie intégré aux entraînements seniors",
      "Umwigishwa w'akademi ahabwa umwanya mu myitozo y'ikipe nkuru",
    ),
    summary: L(
      "Placeholder — A standout from the Viking FC academy has been invited to first-team training after strong youth performances. The club continues to build a pathway from the centre de formation into the GSAL-League squad.",
      "Placeholder — Un joueur marquant de l'académie Viking FC a été invité aux entraînements de l'équipe première après de solides performances chez les jeunes. Le club poursuit la passerelle vers l'effectif GSAL-League.",
      "Placeholder — Umukinnyi w'akademi ya Viking FC ahamagawe mu myitozo y'ikipe nkuru nyuma y'imikino myiza mu bato. Ikipe ikomeza inzira kuva mu rwego rw'abato kugeza muri GSAL-League.",
    ),
    content: L(
      "Placeholder — A standout from the Viking FC academy has been invited to first-team training after strong youth performances. The club continues to build a pathway from the centre de formation into the GSAL-League squad while sitting top of the third-division table.",
      "Placeholder — Un joueur marquant de l'académie Viking FC a été invité aux entraînements de l'équipe première après de solides performances chez les jeunes. Le club poursuit la passerelle vers l'effectif GSAL-League tout en menant le classement de troisième division.",
      "Placeholder — Umukinnyi w'akademi ya Viking FC ahamagawe mu myitozo y'ikipe nkuru nyuma y'imikino myiza mu bato. Ikipe ikomeza inzira kuva mu rwego rw'abato kugeza muri GSAL-League, ikaba iyoboye urutonde rw'icyiciro cya gatatu.",
    ),
    category: "academy",
    image: images.stretch,
    date: "2026-08-12",
  },
  {
    id: "n1",
    slug: "gsal-leaders-after-j10",
    title: L(
      "Viking FC top of GSAL-League after 10 matchdays",
      "Viking FC en tête de la GSAL-League après 10 journées",
      "Viking FC iyoboye GSAL-League nyuma y'imikino 10",
    ),
    summary: L(
      "First place with 10 wins from 10, 30 points, 27 goals scored and 4 conceded (+23).",
      "Première place avec 10 victoires en 10 matchs, 30 points, 27 buts marqués et 4 encaissés (+23).",
      "Umwanya wa mbere n'intsinzi 10 mu mikino 10, amanota 30, ibitego 27 byatsinzwe na 4 byakiriwe (+23).",
    ),
    content: L(
      "Viking FC lead the GSAL-League standings after 10 matchdays. The club remains unbeaten with 10 victories, 30 points, 27 goals scored, 4 conceded and a goal difference of +23.",
      "Viking FC mène le classement de la GSAL-League après 10 journées. Le club reste invaincu avec 10 victoires, 30 points, 27 buts marqués, 4 encaissés et une différence de +23.",
      "Viking FC iyoboye urutonde rwa GSAL-League nyuma y'imikino 10. Ikipe ntiyatsinzwe, ifite intsinzi 10, amanota 30, ibitego 27, byakiriwe 4 n'itandukaniro +23.",
    ),
    category: "club",
    image: images.celebrate,
    date: "2026-08-10",
  },
  {
    id: "n2",
    slug: "viking-5-0-black-star",
    title: L(
      "Viking FC 5–0 Black Star de Kigali",
      "Viking FC 5–0 Black Star de Kigali",
      "Viking FC 5–0 Black Star de Kigali",
    ),
    summary: L(
      "Matchday 8 of the GSAL-League at Ejo Heza Stadium (8 August 2026).",
      "8e journée de GSAL-League à Ejo Heza Stadium (8 août 2026).",
      "Umukino wa 8 wa GSAL-League kuri Ejo Heza Stadium (8 Kanama 2026).",
    ),
    content: L(
      "Viking FC defeated Black Star de Kigali 5–0 in GSAL-League matchday 8 at Ejo Heza Stadium on 8 August 2026.",
      "Viking FC a battu Black Star de Kigali 5–0 lors de la 8e journée de GSAL-League à Ejo Heza Stadium le 8 août 2026.",
      "Viking FC yatsinze Black Star de Kigali 5–0 mu mukino wa 8 wa GSAL-League kuri Ejo Heza Stadium ku ya 8 Kanama 2026.",
    ),
    category: "matchReports",
    image: images.blueKit,
    date: "2026-08-08",
  },
  {
    id: "n3",
    slug: "viking-2-0-aspk",
    title: L("Viking FC 2–0 ASPK", "Viking FC 2–0 ASPK", "Viking FC 2–0 ASPK"),
    summary: L(
      "GSAL-League victory for Viking FC against ASPK.",
      "Victoire de Viking FC en GSAL-League face à l'ASPK.",
      "Intsinzi ya Viking FC muri GSAL-League kurwanya ASPK.",
    ),
    content: L(
      "Viking FC beat ASPK 2–0 in the GSAL-League.",
      "Viking FC a battu l'ASPK 2–0 en GSAL-League.",
      "Viking FC yatsinze ASPK 2–0 muri GSAL-League.",
    ),
    category: "matchReports",
    image: images.squad,
    date: "",
  },
  {
    id: "n4",
    slug: "viking-1-0-black-panthers",
    title: L(
      "Viking FC 1–0 AS Black Panthers",
      "Viking FC 1–0 AS Black Panthers",
      "Viking FC 1–0 AS Black Panthers",
    ),
    summary: L(
      "GSAL-League win for Viking FC against AS Black Panthers.",
      "Victoire de Viking FC en GSAL-League face à AS Black Panthers.",
      "Intsinzi ya Viking FC muri GSAL-League kurwanya AS Black Panthers.",
    ),
    content: L(
      "Viking FC defeated AS Black Panthers 1–0 in the GSAL-League.",
      "Viking FC a battu AS Black Panthers 1–0 en GSAL-League.",
      "Viking FC yatsinze AS Black Panthers 1–0 muri GSAL-League.",
    ),
    category: "matchReports",
    image: images.pitch,
    date: "",
  },
  {
    id: "n5",
    slug: "viking-3-0-mapane",
    title: L(
      "Viking FC 3–0 Mapane FC",
      "Viking FC 3–0 Mapane FC",
      "Viking FC 3–0 Mapane FC",
    ),
    summary: L(
      "GSAL-League victory for Viking FC against Mapane FC.",
      "Victoire de Viking FC en GSAL-League face à Mapane FC.",
      "Intsinzi ya Viking FC muri GSAL-League kurwanya Mapane FC.",
    ),
    content: L(
      "Viking FC beat Mapane FC 3–0 in the GSAL-League.",
      "Viking FC a battu Mapane FC 3–0 en GSAL-League.",
      "Viking FC yatsinze Mapane FC 3–0 muri GSAL-League.",
    ),
    category: "matchReports",
    image: images.run,
    date: "",
  },
  {
    id: "n6",
    slug: "viking-2-0-la-selecao",
    title: L(
      "Viking FC 2–0 La Seleção",
      "Viking FC 2–0 La Seleção",
      "Viking FC 2–0 La Seleção",
    ),
    summary: L(
      "GSAL-League win for Viking FC against La Seleção.",
      "Victoire de Viking FC en GSAL-League face à La Seleção.",
      "Intsinzi ya Viking FC muri GSAL-League kurwanya La Seleção.",
    ),
    content: L(
      "Viking FC defeated La Seleção 2–0 in the GSAL-League.",
      "Viking FC a battu La Seleção 2–0 en GSAL-League.",
      "Viking FC yatsinze La Seleção 2–0 muri GSAL-League.",
    ),
    category: "matchReports",
    image: images.jog,
    date: "",
  },
];

export const leagueTable = [
  { pos: 1, team: "Viking FC", played: 10, won: 10, drawn: 0, lost: 0, gf: 27, ga: 4, gd: 23, pts: 30 },
  { pos: 2, team: "ASPK", played: 10, won: 8, drawn: 1, lost: 1, gf: 31, ga: 7, gd: 24, pts: 25 },
  { pos: 3, team: "RCK", played: 10, won: 6, drawn: 1, lost: 3, gf: 28, ga: 15, gd: 13, pts: 19 },
  { pos: 4, team: "Black Panthers", played: 10, won: 6, drawn: 0, lost: 4, gf: 18, ga: 14, gd: 4, pts: 18 },
  { pos: 5, team: "Mapane FC", played: 10, won: 2, drawn: 3, lost: 5, gf: 11, ga: 19, gd: -8, pts: 9 },
  { pos: 6, team: "AS Training", played: 10, won: 2, drawn: 2, lost: 6, gf: 9, ga: 19, gd: -10, pts: 8 },
  { pos: 7, team: "Black Star", played: 10, won: 1, drawn: 3, lost: 6, gf: 12, ga: 23, gd: -11, pts: 6 },
  { pos: 8, team: "La Seleção", played: 10, won: 0, drawn: 0, lost: 10, gf: 9, ga: 44, gd: -35, pts: 0 },
];

export const sponsors = [
  { name: "Gitera Safari", logo: images.logo },
  { name: "Madeleine Group", logo: images.logo },
];

// TODO: replace with real transfer data from club communications
export const mercatoPlaceholders = [
  {
    id: "m1",
    initials: "JP",
    name: "Player Name A",
    role: "Forward",
    detail: "Placeholder — incoming signing",
    status: "signing" as const,
  },
  {
    id: "m2",
    initials: "MK",
    name: "Player Name B",
    role: "Midfielder",
    detail: "Placeholder — from GSAL club",
    status: "official" as const,
  },
  {
    id: "m3",
    initials: "DN",
    name: "Player Name C",
    role: "Defender",
    detail: "Placeholder — contract extension",
    status: "extension" as const,
  },
  {
    id: "m4",
    initials: "ST",
    name: "Staff Name D",
    role: "Assistant Coach",
    detail: "Placeholder — technical staff",
    status: "official" as const,
  },
  {
    id: "m5",
    initials: "RW",
    name: "Player Name E",
    role: "Goalkeeper",
    detail: "Placeholder — academy promotion",
    status: "trial" as const,
  },
  // TODO: replace with real transfer data
  {
    id: "m6",
    initials: "EH",
    name: "Erik Hakizimana",
    role: "Defender",
    detail: "Placeholder — from regional GSAL side",
    status: "signing" as const,
  },
  // TODO: replace with real transfer data
  {
    id: "m7",
    initials: "LN",
    name: "Lars Niyonzima",
    role: "Midfielder",
    detail: "Placeholder — creative midfielder target",
    status: "official" as const,
  },
  // TODO: replace with real transfer data
  {
    id: "m8",
    initials: "SB",
    name: "Sven Björnsson",
    role: "Forward",
    detail: "Placeholder — trialist striker",
    status: "trial" as const,
  },
  // TODO: replace with real transfer data
  {
    id: "m9",
    initials: "OU",
    name: "Olivier Uwase",
    role: "Goalkeeper",
    detail: "Placeholder — backup goalkeeper option",
    status: "signing" as const,
  },
  // TODO: replace with real transfer data
  {
    id: "m10",
    initials: "KM",
    name: "Knut Mugisha",
    role: "Fitness Coach",
    detail: "Placeholder — technical staff reinforcement",
    status: "extension" as const,
  },
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

const VERB = {
  en: {
    LOGIN: "signed in",
    LOGIN_FAILED: "failed to sign in",
    LOGIN_LOCKOUT: "was locked out after failed sign-ins",
    LOGIN_OTP_SENT: "was sent a login code",
    LOGIN_OTP_OK: "confirmed the login code",
    CREATE: "added",
    UPDATE: "updated",
    DELETE: "deleted",
    STAFF_ACCOUNT_CREATE: "created a login for",
    STAFF_ACCOUNT_SUSPEND: "suspended the login of",
    STAFF_ACCOUNT_ACTIVATE: "activated the login of",
    STAFF_ACCOUNT_REMOVE: "removed the login of",
    STAFF_ARCHIVE: "archived",
    STAFF_INVITE_RESEND: "resent the invitation to",
    STAFF_PASSWORD_RESET: "reset the password of",
    MOVE_FORMER: "moved to former players",
    RESTORE_FORMER: "restored to the squad",
    ROLE_CREATE: "created the role",
    ROLE_UPDATE: "updated the role",
    ROLE_DELETE: "deleted the role",
    ROLE_ASSIGN: "assigned the role",
    ROLE_REASSIGN_DELETE: "moved people and deleted the role",
    USER_ARCHIVE: "archived the account of",
    USER_DELETE: "deleted the account of",
  },
  fr: {
    LOGIN: "s'est connecté",
    LOGIN_FAILED: "n'a pas pu se connecter",
    LOGIN_LOCKOUT: "a été bloqué après des échecs de connexion",
    LOGIN_OTP_SENT: "a reçu un code de connexion",
    LOGIN_OTP_OK: "a confirmé le code de connexion",
    CREATE: "a ajouté",
    UPDATE: "a modifié",
    DELETE: "a supprimé",
    STAFF_ACCOUNT_CREATE: "a créé un compte pour",
    STAFF_ACCOUNT_SUSPEND: "a suspendu le compte de",
    STAFF_ACCOUNT_ACTIVATE: "a activé le compte de",
    STAFF_ACCOUNT_REMOVE: "a retiré le compte de",
    STAFF_ARCHIVE: "a archivé",
    STAFF_INVITE_RESEND: "a renvoyé l'invitation à",
    STAFF_PASSWORD_RESET: "a réinitialisé le mot de passe de",
    MOVE_FORMER: "a envoyé chez les anciens",
    RESTORE_FORMER: "a ramené dans l'effectif",
    ROLE_CREATE: "a créé le rôle",
    ROLE_UPDATE: "a modifié le rôle",
    ROLE_DELETE: "a supprimé le rôle",
    ROLE_ASSIGN: "a attribué le rôle",
    ROLE_REASSIGN_DELETE: "a déplacé les personnes et supprimé le rôle",
    USER_ARCHIVE: "a archivé le compte de",
    USER_DELETE: "a supprimé le compte de",
  },
  rw: {
    LOGIN: "yinjiye",
    LOGIN_FAILED: "yananiwe kwinjira",
    LOGIN_LOCKOUT: "yafunzwe nyuma yo kunanirwa kwinjira",
    LOGIN_OTP_SENT: "yoherejwe kode yo kwinjira",
    LOGIN_OTP_OK: "yemeje kode yo kwinjira",
    CREATE: "yongeyeho",
    UPDATE: "yahinduye",
    DELETE: "yasibye",
    STAFF_ACCOUNT_CREATE: "yafunguye konti ya",
    STAFF_ACCOUNT_SUSPEND: "yahagaritse konti ya",
    STAFF_ACCOUNT_ACTIVATE: "yafunguye konti ya",
    STAFF_ACCOUNT_REMOVE: "yakuweho konti ya",
    STAFF_ARCHIVE: "yabikiye",
    STAFF_INVITE_RESEND: "yongera kohereza ubutumire kuri",
    STAFF_PASSWORD_RESET: "yahinduye ijambobanga rya",
    MOVE_FORMER: "yohereje mu bakinnyi ba kera",
    RESTORE_FORMER: "yagaruye mu ikipe",
    ROLE_CREATE: "yaremye uruhare",
    ROLE_UPDATE: "yahinduye uruhare",
    ROLE_DELETE: "yasibye uruhare",
    ROLE_ASSIGN: "yahawe uruhare",
    ROLE_REASSIGN_DELETE: "yimuye abantu akasiba uruhare",
    USER_ARCHIVE: "yabikiye konti ya",
    USER_DELETE: "yasibye konti ya",
  },
};

const NOISE = new Set(["SUSPENDED", "ACTIVE", "INVITED", "NONE", "ARCHIVED", "true", "false"]);

function tidy(value) {
  if (value == null) return "";
  const text = String(value).trim().replace(/^"|"$/g, "");
  if (!text || text === "null" || NOISE.has(text)) return "";
  return text;
}

function verb(locale, action) {
  const pack = VERB[locale] || VERB.en;
  if (pack[action]) return pack[action];
  return String(action || "updated").toLowerCase().replaceAll("_", " ");
}

const ROLE = {
  en: {
    PRESIDENT: "President",
    GENERAL_MANAGER: "General Manager",
    FINANCE_MANAGER: "Finance Manager",
    ASSISTANT_MANAGER: "Assistant Manager",
    HEAD_COACH: "Head Coach",
    ASSISTANT_COACH: "Assistant Coach",
    GOALKEEPER_COACH: "Goalkeeper Coach",
    FITNESS_COACH: "Fitness Coach",
    DOCTOR: "Doctor",
    PHYSIOTHERAPIST: "Physiotherapist",
    KIT_MANAGER: "Kit Manager",
    TEAM_MANAGER: "Team Manager",
    MARKETING_MANAGER: "Marketing Manager",
    SOCIAL_MEDIA_MANAGER: "Social Media Manager",
    PLAYER: "Player",
  },
  fr: {
    PRESIDENT: "Président",
    GENERAL_MANAGER: "Directeur général",
    FINANCE_MANAGER: "Directeur financier",
    ASSISTANT_MANAGER: "Directeur adjoint",
    HEAD_COACH: "Entraîneur principal",
    ASSISTANT_COACH: "Entraîneur adjoint",
    GOALKEEPER_COACH: "Entraîneur des gardiens",
    FITNESS_COACH: "Préparateur physique",
    DOCTOR: "Médecin",
    PHYSIOTHERAPIST: "Kinésithérapeute",
    KIT_MANAGER: "Intendant",
    TEAM_MANAGER: "Team manager",
    MARKETING_MANAGER: "Responsable marketing",
    SOCIAL_MEDIA_MANAGER: "Responsable des réseaux",
    PLAYER: "Joueur",
  },
  rw: {
    PRESIDENT: "Perezida",
    GENERAL_MANAGER: "Umuyobozi mukuru",
    FINANCE_MANAGER: "Umuyobozi w'imari",
    ASSISTANT_MANAGER: "Umuyobozi wungirije",
    HEAD_COACH: "Umutoza mukuru",
    ASSISTANT_COACH: "Umutoza wungirije",
    GOALKEEPER_COACH: "Umutoza w'abakinnyi",
    FITNESS_COACH: "Umutoza w'imbaraga",
    DOCTOR: "Muganga",
    PHYSIOTHERAPIST: "Umufasha w'ubuvuzi",
    KIT_MANAGER: "Umuyobozi w'ibikoresho",
    TEAM_MANAGER: "Umuyobozi w'ikipe",
    MARKETING_MANAGER: "Umuyobozi w'itangazamakuru",
    SOCIAL_MEDIA_MANAGER: "Umuyobozi w'imbuga",
    PLAYER: "Umukinnyi",
  },
};

export function roleLabel(locale, code) {
  if (!code) return "";
  const pack = ROLE[locale] || ROLE.en;
  if (pack[code]) return pack[code];
  return String(code).replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function activitySentence(locale, item) {
  const email = tidy(item.actor);
  const named = tidy(item.actorName);
  const who = named || (email && email !== "system" && !email.includes("@") ? email : "") || (email && email !== "system" && !named ? email.split("@")[0] : "") || "Someone";
  const subject = tidy(item.targetName);
  const line = verb(locale, item.action);
  if (!subject || subject === item.action || subject === who) return `${who} ${line}`;
  return `${who} ${line} ${subject}`;
}

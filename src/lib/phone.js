const TEXT = {
  en: {
    short: "Enter all 9 digits.",
    prefix: "A Rwanda mobile starts with 72, 73, 78 or 79.",
  },
  fr: {
    short: "Saisis les 9 chiffres.",
    prefix: "Un mobile rwandais commence par 72, 73, 78 ou 79.",
  },
  rw: {
    short: "Andika imibare 9 yose.",
    prefix: "Telefoni ya Rwanda itangira na 72, 73, 78 cyangwa 79.",
  },
};

export function phoneDigits(value) {
  return String(value || "").replace(/\D/g, "").replace(/^250/, "").replace(/^0/, "").slice(0, 9);
}

export function phoneProblem(value, locale) {
  const lang = locale || (typeof window === "undefined" ? "en" : (window.localStorage.getItem("vfc_lang") || window.localStorage.getItem("viking-locale") || "en"));
  const digits = phoneDigits(value);
  if (!digits) return "";
  const text = TEXT[lang] || TEXT.en;
  if (digits.length < 9) return text.short;
  if (!/^7[2389]\d{7}$/.test(digits)) return text.prefix;
  return "";
}

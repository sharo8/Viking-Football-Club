package rw.vikingfc.club.common;

/**
 * Picks the label in the requested language, then the language it was written in.
 * A missing translation is never an error and never a blank when a main name exists.
 */
public final class LabelText {

    private LabelText() {
    }

    public static String pick(String locale, String sourceLang, String main, String nameEn, String nameFr, String nameRw) {
        String direct = value(locale, nameEn, nameFr, nameRw);
        if (!direct.isBlank()) {
            return direct;
        }
        String written = value(sourceLang, nameEn, nameFr, nameRw);
        if (!written.isBlank()) {
            return written;
        }
        if (main != null && !main.isBlank()) {
            return main.trim();
        }
        if (nameEn != null && !nameEn.isBlank()) {
            return nameEn.trim();
        }
        if (nameFr != null && !nameFr.isBlank()) {
            return nameFr.trim();
        }
        return nameRw == null ? "" : nameRw.trim();
    }

    private static String value(String lang, String nameEn, String nameFr, String nameRw) {
        String raw = switch (lang == null ? "" : lang) {
            case "fr" -> nameFr;
            case "rw" -> nameRw;
            case "en" -> nameEn;
            default -> "";
        };
        return raw == null ? "" : raw.trim();
    }
}

package rw.vikingfc.club.contract;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

public final class CaptainSpan {

    public static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");

    private CaptainSpan() {
    }

    public static String format(Instant start, Instant end, Instant now, String lang) {
        if (start == null) {
            return "–";
        }
        Instant until = end == null ? now : end;
        if (until == null || !until.isAfter(start)) {
            return phrase(lang, "same");
        }
        if (Duration.between(start, until).toHours() < 24) {
            LocalDate from = start.atZone(KIGALI).toLocalDate();
            LocalDate to = until.atZone(KIGALI).toLocalDate();
            return from.equals(to) ? phrase(lang, "same") : phrase(lang, "less");
        }
        Period period = Period.between(start.atZone(KIGALI).toLocalDate(), until.atZone(KIGALI).toLocalDate());
        int months = period.getYears() * 12 + period.getMonths();
        int days = period.getDays();
        if (months > 0) {
            String month = months == 1 ? phrase(lang, "month") : months + " " + phrase(lang, "months");
            return days == 0 ? month : month + " " + dayPhrase(days, lang);
        }
        if (days >= 7) {
            int weeks = days / 7;
            int rest = days % 7;
            String week = weeks == 1 ? phrase(lang, "week") : weeks + " " + phrase(lang, "weeks");
            return rest == 0 ? week : week + " " + dayPhrase(rest, lang);
        }
        return dayPhrase(Math.max(days, 1), lang);
    }

    private static String dayPhrase(int days, String lang) {
        return days == 1 ? phrase(lang, "day") : days + " " + phrase(lang, "days");
    }

    private static String phrase(String lang, String key) {
        boolean fr = "fr".equals(lang);
        boolean rw = "rw".equals(lang);
        return switch (key) {
            case "same" -> fr ? "Le même jour" : rw ? "Uwo munsi" : "Same day";
            case "less" -> fr ? "Moins d'un jour" : rw ? "Munsi y'umunsi 1" : "Less than 1 day";
            case "day" -> fr ? "1 jour" : rw ? "Umunsi 1" : "1 day";
            case "days" -> fr ? "jours" : rw ? "iminsi" : "days";
            case "week" -> fr ? "1 semaine" : rw ? "Icyumweru 1" : "1 week";
            case "weeks" -> fr ? "semaines" : rw ? "ibyumweru" : "weeks";
            case "month" -> fr ? "1 mois" : rw ? "Ukwezi 1" : "1 month";
            case "months" -> fr ? "mois" : rw ? "amezi" : "months";
            default -> key;
        };
    }
}

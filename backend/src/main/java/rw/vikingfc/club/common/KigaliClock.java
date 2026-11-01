package rw.vikingfc.club.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class KigaliClock {

    public static final ZoneId ZONE = ZoneId.of("Africa/Kigali");

    private KigaliClock() {
    }

    public static String format(Instant instant, String lang) {
        if (instant == null) {
            return "";
        }
        Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        return DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", locale).withZone(ZONE).format(instant);
    }
}

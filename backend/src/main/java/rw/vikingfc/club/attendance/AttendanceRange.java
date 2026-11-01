package rw.vikingfc.club.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;

public final class AttendanceRange {

    private AttendanceRange() {
    }

    public static LocalDate[] bounds(String preset, LocalDate from, LocalDate to, LocalDate today) {
        String key = preset == null || preset.isBlank() ? "month" : preset;
        return switch (key) {
            case "today" -> new LocalDate[]{today, today};
            case "week" -> new LocalDate[]{today.with(DayOfWeek.MONDAY), today};
            case "year" -> new LocalDate[]{today.withDayOfYear(1), today};
            case "all" -> new LocalDate[]{null, null};
            case "custom" -> new LocalDate[]{from, to};
            default -> new LocalDate[]{today.withDayOfMonth(1), today};
        };
    }

    public static boolean in(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        return to == null || !date.isAfter(to);
    }

    public static String bucket(LocalDate date, String grain) {
        String key = grain == null ? "day" : grain;
        return switch (key) {
            case "week" -> date.getYear() + "-W" + date.get(WeekFields.ISO.weekOfWeekBasedYear());
            case "month" -> date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            case "year" -> String.valueOf(date.getYear());
            default -> date.toString();
        };
    }
}

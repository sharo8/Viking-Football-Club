package rw.vikingfc.club.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.attendance.AttendanceDeskService;
import rw.vikingfc.club.attendance.AttendanceRange;
import rw.vikingfc.club.attendance.AttendanceReport;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.sanction.FineService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final AttendanceDeskService desk;
    private final AttendanceReport report;
    private final FineService fines;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ExportFile run(Map<String, Object> body) {
        String resource = String.valueOf(body.getOrDefault("resource", "FINES")).toUpperCase(Locale.ROOT);
        String format = String.valueOf(body.getOrDefault("format", "pdf")).toLowerCase(Locale.ROOT);
        String preset = preset(body);
        String players = people(body);
        LocalDate today = LocalDate.now(clock);
        LocalDate[] bounds = AttendanceRange.bounds(preset, date(body, "from"), date(body, "to"), today);
        String period = bounds[0] == null ? "All time" : bounds[0] + " – " + bounds[1];
        String mode = "ATTENDANCE".equals(resource) ? "attendance" : "BOTH".equals(resource) ? "both" : "fines";
        byte[] bytes;
        String name;
        if ("fines".equals(mode)) {
            bytes = report.finesPack(format, "en", "Club office", period, fineLines(bounds, players));
            name = "VIKING-FC_Fines_" + preset + "." + ext(format);
        } else if ("both".equals(mode) && "csv".equals(format)) {
            byte[] attendance = desk.export("csv", "en", "players", preset, bounds[0], bounds[1], "day",
                    null, null, null, null, null, null, null, null, null, null, null, players, "attendance");
            byte[] fineFile = report.finesPack("csv", "en", "Club office", period, fineLines(bounds, players));
            bytes = zip(attendance, fineFile);
            name = "VIKING-FC_Attendance-and-Fines_" + preset + ".zip";
            format = "zip";
        } else {
            bytes = desk.export(format, "en", "players", preset, bounds[0], bounds[1], "day",
                    null, null, null, null, null, null, null, null, null, null, null, players, mode);
            name = "VIKING-FC_" + ("attendance".equals(mode) ? "Attendance" : "Attendance-and-Fines") + "_" + preset + "." + ext(format);
        }
        auditService.record("EXPORT", resource, null, null, format + " " + preset, null);
        return new ExportFile(bytes, name, media(format));
    }

    private List<AttendanceReport.FineLine> fineLines(LocalDate[] bounds, String players) {
        Set<String> ids = players == null || players.isBlank() ? Set.of() : Set.of(players.split(","));
        List<AttendanceReport.FineLine> lines = new ArrayList<>();
        for (Map<String, Object> row : fines.ledger()) {
            LocalDate date = row.get("date") == null ? null : LocalDate.parse(String.valueOf(row.get("date")));
            if (bounds[0] != null && !AttendanceRange.in(date, bounds[0], bounds[1])) {
                continue;
            }
            if (!ids.isEmpty() && !ids.contains(String.valueOf(row.get("playerId")))) {
                continue;
            }
            lines.add(new AttendanceReport.FineLine(
                    text(row.get("player")),
                    row.get("rule") == null || String.valueOf(row.get("rule")).isBlank() ? text(row.get("reason")) : "Rule " + row.get("rule"),
                    text(row.get("source")),
                    date == null ? "" : date.toString(),
                    money(row.get("amount")),
                    money(row.get("paid")),
                    money(row.get("balance")),
                    String.valueOf(row.get("status"))));
        }
        return lines;
    }

    private static byte[] zip(byte[] attendance, byte[] fines) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(out);
            zip.putNextEntry(new ZipEntry("attendance.csv"));
            zip.write(attendance);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("fines.csv"));
            zip.write(fines);
            zip.closeEntry();
            zip.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build the export archive", exception);
        }
    }

    private static String preset(Map<String, Object> body) {
        Object scope = body.get("scope");
        if (scope instanceof Map<?, ?> map && map.get("preset") != null) {
            return String.valueOf(map.get("preset"));
        }
        return scope == null ? "all" : String.valueOf(scope);
    }

    private static String people(Map<String, Object> body) {
        Object people = body.get("people");
        if (people instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return people == null ? "" : String.valueOf(people);
    }

    private static LocalDate date(Map<String, Object> body, String key) {
        Object scope = body.get("scope");
        Object value = scope instanceof Map<?, ?> map ? map.get(key) : body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static long money(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.longValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String ext(String format) {
        return "pdf".equals(format) ? "pdf" : "csv".equals(format) ? "csv" : "xlsx";
    }

    private static String media(String format) {
        return switch (format) {
            case "pdf" -> "application/pdf";
            case "csv" -> "text/csv";
            case "zip" -> "application/zip";
            default -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }

    public record ExportFile(byte[] bytes, String name, String media) {
    }
}

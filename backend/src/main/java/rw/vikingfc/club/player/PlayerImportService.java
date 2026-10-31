package rw.vikingfc.club.player;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.util.SecurityUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PlayerImportService {

    private static final List<String> COLUMNS = List.of(
            "fullName", "knownAs", "jersey", "position", "dateOfBirth", "nationality", "foot",
            "heightCm", "weightKg", "status", "phone", "email", "preferredLanguage", "address",
            "emergencyName", "emergencyRelationship", "emergencyPhone", "contractStart", "contractEnd",
            "seasonalPay", "advance", "socialFund", "sfExempt", "releaseClause", "marketValue");

    private final PlayerOfficeService officeService;
    private final PlayerRepository playerRepository;
    private final AuditService auditService;
    private final Map<String, Preview> previews = new ConcurrentHashMap<>();
    private final DataFormatter formatter = new DataFormatter();

    public byte[] template() {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Players");
            Row header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.size(); i++) {
                header.createCell(i).setCellValue(COLUMNS.get(i));
            }
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("Example Player");
            example.createCell(2).setCellValue(99);
            example.createCell(3).setCellValue("FORWARD");
            example.createCell(4).setCellValue("2000-01-15");
            example.createCell(5).setCellValue("Rwanda");
            Sheet help = book.createSheet("Instructions");
            help.createRow(0).createCell(0).setCellValue("Dates: yyyy-MM-dd or dd/MM/yyyy. Yes/No/Oui/Non for sfExempt. Photo cannot be imported.");
            book.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw BusinessException.badRequest("The template could not be built");
        }
    }

    public Map<String, Object> preview(MultipartFile file) {
        if (!SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can import players");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw BusinessException.badRequest("The file is larger than 5 MB");
        }
        List<Map<String, String>> table = read(file);
        if (table.size() > 500) {
            throw BusinessException.badRequest("A file can contain at most 500 rows");
        }
        List<Map<String, Object>> squad = officeService.directory("", "", "", "", "", false, false, false, "jersey", "asc", 0, 100).content();
        List<Map<String, Object>> rows = new ArrayList<>();
        int index = 2;
        for (Map<String, String> cells : table) {
            rows.add(judge(index++, cells, squad, rows));
        }
        String id = UUID.randomUUID().toString();
        previews.put(id, new Preview(file.getOriginalFilename(), rows, Instant.now()));
        return Map.of("importId", id, "rows", rows, "summary", summary(rows));
    }

    public Map<String, Object> commit(String importId) {
        Preview preview = previews.get(importId);
        if (preview == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "This import preview has expired", "IMPORT_EXPIRED", null);
        }
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> names = new ArrayList<>();
        for (Map<String, Object> row : preview.rows) {
            String status = String.valueOf(row.get("status"));
            if (!"NEW".equals(status) && !"UPDATE".equals(status)) {
                skipped++;
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = new LinkedHashMap<>((Map<String, Object>) row.get("body"));
            if ("UPDATE".equals(status)) {
                body.put("id", row.get("playerId"));
                body.put("confirmDuplicate", true);
            }
            try {
                officeService.save(body, false);
                if ("NEW".equals(status)) {
                    created++;
                    names.add(String.valueOf(body.get("fullName")));
                } else {
                    updated++;
                }
            } catch (RuntimeException ex) {
                skipped++;
                row.put("status", "REJECTED");
                row.put("reason", ex.getMessage());
            }
        }
        auditService.record("IMPORT", "Player", null, null, preview.fileName + " created=" + created + " updated=" + updated, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("createdNames", names);
        return result;
    }

    public byte[] errors(String importId) {
        Preview preview = previews.get(importId);
        if (preview == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "This import preview has expired", "IMPORT_EXPIRED", null);
        }
        StringBuilder csv = new StringBuilder("row,status,reason,fullName\n");
        for (Map<String, Object> row : preview.rows) {
            if (!"REJECTED".equals(row.get("status"))) {
                continue;
            }
            csv.append(row.get("row")).append(',').append(row.get("status")).append(",\"")
                    .append(String.valueOf(row.get("reason")).replace("\"", "'")).append("\",")
                    .append(row.get("fullName")).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Object> judge(int number, Map<String, String> cells, List<Map<String, Object>> squad, List<Map<String, Object>> earlier) {
        Map<String, Object> body = new LinkedHashMap<>();
        cells.forEach(body::put);
        String name = cells.getOrDefault("fullName", "");
        List<String> reasons = new ArrayList<>();
        if (name.isBlank()) {
            reasons.add("Row " + number + ", column fullName: required");
        }
        String email = cells.getOrDefault("email", "");
        if (!email.isBlank() && !email.contains("@")) {
            reasons.add("Row " + number + ", column Email: invalid format");
        }
        List<Map<String, Object>> matches = squad.stream().filter(player -> matches(player, cells)).toList();
        long inFile = earlier.stream().filter(row -> name.equalsIgnoreCase(String.valueOf(row.get("fullName"))) && "NEW".equals(row.get("status"))).count();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("row", number);
        view.put("fullName", name);
        view.put("body", body);
        if (!reasons.isEmpty() || matches.size() > 1 || inFile > 0) {
            if (matches.size() > 1) {
                reasons.add("Row " + number + " matches more than one player");
            }
            if (inFile > 0) {
                reasons.add("Row " + number + " duplicates another row in this file");
            }
            view.put("status", "REJECTED");
            view.put("reason", String.join("; ", reasons));
            return view;
        }
        if (matches.size() == 1) {
            view.put("status", cells.values().stream().allMatch(String::isBlank) ? "UNCHANGED" : "UPDATE");
            view.put("playerId", matches.get(0).get("id"));
            view.put("reason", "Matched " + matches.get(0).get("fullName"));
            return view;
        }
        if ("true".equals(String.valueOf(body.get("sfExempt")))) {
            body.put("sfExempt", true);
        }
        body.put("importing", true);
        List<Map<String, Object>> issues = PlayerProfileRules.issues(body, true, true, true, java.time.LocalDate.now());
        if (!issues.isEmpty()) {
            view.put("status", "REJECTED");
            view.put("reason", issues.stream().map(item -> "Row " + number + ", column " + item.get("field") + ": " + item.get("message")).reduce((a, b) -> a + "; " + b).orElse("Invalid"));
            return view;
        }
        view.put("status", "NEW");
        view.put("reason", "Photo missing");
        return view;
    }

    private boolean matches(Map<String, Object> player, Map<String, String> cells) {
        String email = cells.getOrDefault("email", "");
        if (!email.isBlank() && email.equalsIgnoreCase(String.valueOf(player.get("email")))) {
            return true;
        }
        String name = cells.getOrDefault("fullName", "");
        String jersey = cells.getOrDefault("jersey", "");
        boolean sameName = name.equalsIgnoreCase(String.valueOf(player.get("fullName")));
        boolean sameJersey = !jersey.isBlank() && jersey.equals(String.valueOf(player.get("jersey")));
        String dob = cells.getOrDefault("dateOfBirth", "");
        boolean sameDob = !dob.isBlank() && dob.equals(String.valueOf(player.get("dateOfBirth")));
        return (sameName && sameJersey) || (sameName && sameDob);
    }

    private Map<String, Integer> summary(List<Map<String, Object>> rows) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : List.of("NEW", "UPDATE", "REJECTED", "UNCHANGED")) {
            counts.put(status, (int) rows.stream().filter(row -> status.equals(row.get("status"))).count());
        }
        return counts;
    }

    private List<Map<String, String>> read(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".xlsx")) {
                return readExcel(file);
            }
            return readCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw BusinessException.badRequest("The file could not be read");
        }
    }

    private List<Map<String, String>> readExcel(MultipartFile file) throws Exception {
        try (Workbook book = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = book.getSheet("Player Register");
            if (sheet == null) {
                sheet = book.getSheetAt(0);
            }
            return rows(sheet);
        }
    }

    private List<Map<String, String>> rows(Sheet sheet) {
        Row header = sheet.getRow(0);
        List<String> keys = new ArrayList<>();
        for (Cell cell : header) {
            keys.add(canonical(formatter.formatCellValue(cell)));
        }
        List<Map<String, String>> table = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            Map<String, String> cells = new LinkedHashMap<>();
            boolean any = false;
            for (int column = 0; column < keys.size(); column++) {
                String value = formatter.formatCellValue(row.getCell(column)).trim();
                if (!value.isBlank()) {
                    any = true;
                }
                cells.put(keys.get(column), normalize(keys.get(column), value));
            }
            if (any) {
                table.add(cells);
            }
        }
        return table;
    }

    private List<Map<String, String>> readCsv(String csv) {
        String[] lines = csv.split("\\R");
        if (lines.length == 0) {
            return List.of();
        }
        String[] header = lines[0].split(",");
        List<Map<String, String>> table = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] cells = lines[i].split(",", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < header.length && column < cells.length; column++) {
                String key = canonical(header[column]);
                row.put(key, normalize(key, cells[column].replace("\"", "").trim()));
            }
            table.add(row);
        }
        return table;
    }

    private String canonical(String header) {
        String key = header.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
        return switch (key) {
            case "name", "nom", "fullname" -> "fullName";
            case "knownas", "surnom" -> "knownAs";
            case "jersey", "numero", "no" -> "jersey";
            case "position", "poste" -> "position";
            case "dateofbirth", "dob", "naissance" -> "dateOfBirth";
            case "seasonalpay", "pay", "salaire" -> "seasonalPay";
            case "sfexempt", "exemption" -> "sfExempt";
            case "advance", "avance" -> "advance";
            default -> header.trim();
        };
    }

    private String normalize(String key, String value) {
        if (value.isBlank()) {
            return "";
        }
        if ("dateOfBirth".equals(key) || "contractStart".equals(key) || "contractEnd".equals(key)) {
            try {
                if (value.contains("/")) {
                    return LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/yyyy")).toString();
                }
            } catch (RuntimeException ignored) {
                return value;
            }
        }
        if ("sfExempt".equals(key)) {
            String lower = value.toLowerCase(Locale.ROOT);
            return (lower.equals("yes") || lower.equals("oui") || lower.equals("true") || lower.equals("1")) ? "true" : "false";
        }
        if (List.of("jersey", "seasonalPay", "advance", "socialFund", "releaseClause", "marketValue", "heightCm", "weightKg").contains(key)) {
            return value.replace(" ", "").replace(",", "");
        }
        return value;
    }

    private record Preview(String fileName, List<Map<String, Object>> rows, Instant createdAt) {
    }
}

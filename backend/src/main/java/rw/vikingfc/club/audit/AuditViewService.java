package rw.vikingfc.club.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.api.PageResponse;
import rw.vikingfc.club.staff.StaffMember;
import rw.vikingfc.club.staff.StaffMemberRepository;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditViewService {

    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");
    private static final Set<String> NOISE = Set.of(
            "SUSPENDED", "ACTIVE", "INVITED", "NONE", "ARCHIVED", "TRUE", "FALSE", "NULL");

    private final AuditLogRepository audits;
    private final UserRepository users;
    private final StaffMemberRepository staff;

    public PageResponse<Map<String, Object>> list(int page, int size, String query, String role) {
        return list(page, size, query, role, null);
    }

    public PageResponse<Map<String, Object>> list(int page, int size, String query, String role, String preset) {
        Lookup lookup = lookup();
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String roleFilter = role == null ? "" : role.trim();
        List<Map<String, Object>> matched = audits.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 2000))
                .getContent().stream()
                .map(log -> row(log, lookup))
                .filter(row -> roleFilter.isBlank() || roleFilter.equals(String.valueOf(row.get("role"))))
                .filter(row -> inRange(row.get("at"), preset))
                .filter(row -> needle.isBlank() || blob(row).contains(needle))
                .toList();
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, matched.size());
        int to = Math.min(from + safeSize, matched.size());
        int pages = matched.isEmpty() ? 0 : (int) Math.ceil(matched.size() / (double) safeSize);
        return new PageResponse<>(matched.subList(from, to), safePage, safeSize, matched.size(), pages);
    }

    public Map<String, Object> row(AuditLog log) {
        return row(log, lookup());
    }

    private Map<String, Object> row(AuditLog log, Lookup lookup) {
        String action = log.getAction() == null ? "" : log.getAction();
        boolean aboutSelf = action.startsWith("LOGIN") || action.startsWith("PASSWORD");
        String email = clean(log.getActorEmail());
        String name = clean(log.getActorName());
        boolean system = email.isBlank() || "system".equalsIgnoreCase(email);
        if (name.isBlank() && !system) name = lookup.nameByEmail(email);
        User entityUser = "User".equals(log.getEntityName()) ? lookup.user(log.getEntityId()) : null;
        if (name.isBlank() && entityUser != null) {
            name = entityUser.getFullName();
            if (system && entityUser.getEmail() != null) email = entityUser.getEmail();
        }
        String target = "";
        if (!aboutSelf && entityUser != null) target = entityUser.getFullName();
        if (target.isBlank() && !aboutSelf && "StaffMember".equals(log.getEntityName())) {
            target = lookup.staffName(log.getEntityId());
        }
        if (target.isBlank()) target = person(log.getAfterJson(), lookup);
        if (target.isBlank()) target = person(log.getBeforeJson(), lookup);
        if (target.equalsIgnoreCase(name)) target = "";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", log.getId());
        body.put("actor", email);
        body.put("actorName", name);
        body.put("role", log.getActorRole() == null && entityUser != null ? entityUser.getRole().name() : log.getActorRole());
        body.put("action", log.getAction());
        body.put("entity", log.getEntityName());
        body.put("entityId", log.getEntityId());
        body.put("targetName", target);
        body.put("before", clean(log.getBeforeJson()));
        body.put("after", clean(log.getAfterJson()));
        body.put("ip", log.getIpAddress());
        body.put("at", log.getCreatedAt());
        body.put("text", target);
        return body;
    }

    private Lookup lookup() {
        Lookup lookup = new Lookup();
        for (User user : users.findAll()) {
            lookup.byId.put(user.getId(), user);
            if (user.getEmail() != null) lookup.byEmail.put(user.getEmail().toLowerCase(Locale.ROOT), user);
        }
        for (StaffMember member : staff.findAll()) {
            lookup.staff.put(member.getId(), member.getFullName());
        }
        return lookup;
    }

    private static String person(String raw, Lookup lookup) {
        String value = clean(raw);
        if (value.isBlank() || NOISE.contains(value.toUpperCase(Locale.ROOT))) return "";
        if (value.matches("\\d{4,8}")) return "";
        if (value.contains("_") && value.equals(value.toUpperCase(Locale.ROOT))) return "";
        if (value.contains("@")) return lookup.nameByEmail(value);
        return value;
    }

    private static String clean(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\"", "\"").trim();
    }

    private static boolean inRange(Object at, String preset) {
        if (preset == null || preset.isBlank() || "all".equalsIgnoreCase(preset) || !(at instanceof Instant instant)) {
            return preset == null || preset.isBlank() || "all".equalsIgnoreCase(preset);
        }
        LocalDate day = instant.atZone(KIGALI).toLocalDate();
        LocalDate today = LocalDate.now(KIGALI);
        return switch (preset.toLowerCase(Locale.ROOT)) {
            case "today" -> day.equals(today);
            case "week" -> !day.isBefore(today.with(DayOfWeek.MONDAY)) && !day.isAfter(today);
            case "month" -> day.getYear() == today.getYear() && day.getMonth() == today.getMonth();
            case "year" -> day.getYear() == today.getYear();
            default -> true;
        };
    }

    private static String blob(Map<String, Object> row) {
        return (String.valueOf(row.get("actorName")) + " " + row.get("actor") + " " + row.get("action") + " "
                + row.get("targetName") + " " + row.get("role")).toLowerCase(Locale.ROOT);
    }

    private static final class Lookup {
        private final Map<Long, User> byId = new HashMap<>();
        private final Map<String, User> byEmail = new HashMap<>();
        private final Map<Long, String> staff = new HashMap<>();

        private User user(String id) {
            try {
                return id == null ? null : byId.get(Long.valueOf(id));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private String staffName(String id) {
            try {
                return id == null ? "" : staff.getOrDefault(Long.valueOf(id), "");
            } catch (NumberFormatException ex) {
                return "";
            }
        }

        private String nameByEmail(String email) {
            if (email == null) return "";
            User user = byEmail.get(email.toLowerCase(Locale.ROOT));
            return user == null ? "" : user.getFullName();
        }
    }
}

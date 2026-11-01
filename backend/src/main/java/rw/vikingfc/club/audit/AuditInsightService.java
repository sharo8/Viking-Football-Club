package rw.vikingfc.club.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditInsightService {

    private final AuditLogRepository audits;
    private final UserRepository users;
    private final UserSessionRepository sessions;

    public Map<String, Object> insights() {
        List<User> people = users.findByDeletedAtIsNullOrderByFullNameAsc();
        Map<Long, User> byId = new HashMap<>();
        for (User user : people) byId.put(user.getId(), user);

        List<Map<String, Object>> lastLogins = people.stream()
                .filter(user -> user.getLastLoginAt() != null)
                .sorted(Comparator.comparing(User::getLastLoginAt).reversed())
                .limit(12)
                .map(this::loginRow)
                .toList();
        List<Map<String, Object>> never = people.stream()
                .filter(user -> user.getLastLoginAt() == null)
                .limit(12)
                .map(user -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", user.getFullName());
                    row.put("email", user.getEmail());
                    row.put("role", roleOf(user));
                    return row;
                })
                .toList();

        Map<Long, long[]> byUser = new HashMap<>();
        for (UserSession session : sessions.findAll()) {
            if (session.getUserId() == null || !byId.containsKey(session.getUserId())) continue;
            long[] bucket = byUser.computeIfAbsent(session.getUserId(), key -> new long[2]);
            bucket[0] += Math.max(session.getActiveSeconds(), 0);
            bucket[1] += 1;
        }

        List<Map<String, Object>> byPerson = new ArrayList<>();
        Map<String, long[]> byRole = new HashMap<>();
        for (Map.Entry<Long, long[]> entry : byUser.entrySet()) {
            User user = byId.get(entry.getKey());
            String role = roleOf(user);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", user.getFullName());
            row.put("email", user.getEmail());
            row.put("role", role);
            row.put("seconds", entry.getValue()[0]);
            row.put("minutes", entry.getValue()[0] / 60);
            row.put("sessions", entry.getValue()[1]);
            byPerson.add(row);
            long[] roleBucket = byRole.computeIfAbsent(role, key -> new long[2]);
            roleBucket[0] += entry.getValue()[0];
            roleBucket[1] += entry.getValue()[1];
        }
        byPerson.sort(Comparator.comparingLong((Map<String, Object> row) -> (Long) row.get("seconds")).reversed());

        List<Map<String, Object>> roles = new ArrayList<>();
        byRole.forEach((role, bucket) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", role);
            row.put("seconds", bucket[0]);
            row.put("minutes", bucket[0] / 60);
            row.put("sessions", bucket[1]);
            roles.add(row);
        });
        roles.sort(Comparator.comparingLong((Map<String, Object> row) -> (Long) row.get("seconds")).reversed());

        Map<String, Integer> actions = new HashMap<>();
        for (AuditLog log : audits.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1500)).getContent()) {
            if (log.getAction() != null) actions.merge(log.getAction(), 1, Integer::sum);
        }
        List<Map<String, Object>> actionRows = new ArrayList<>();
        actions.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("action", entry.getKey());
                    row.put("count", entry.getValue());
                    actionRows.add(row);
                });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("lastLogins", lastLogins);
        body.put("never", never);
        body.put("people", byPerson.stream().limit(10).toList());
        body.put("roles", roles);
        body.put("actions", actionRows);
        body.put("tracked", !byPerson.isEmpty());
        return body;
    }

    private Map<String, Object> loginRow(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", user.getFullName());
        row.put("email", user.getEmail());
        row.put("role", roleOf(user));
        row.put("at", user.getLastLoginAt());
        return row;
    }

    private static String roleOf(User user) {
        if (user.getRoleCode() != null && !user.getRoleCode().isBlank()) return user.getRoleCode();
        return user.getRole().name();
    }
}

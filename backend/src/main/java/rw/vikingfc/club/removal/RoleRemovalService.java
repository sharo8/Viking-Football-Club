package rw.vikingfc.club.removal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.player.Player;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.user.ClubRole;
import rw.vikingfc.club.user.ClubRoleRepository;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleRemovalService {

    private static final ZoneId KIGALI = ZoneId.of("Africa/Kigali");

    private final ClubRoleRepository roles;
    private final UserRepository users;
    private final PlayerRepository players;
    private final AccountRemovalService accounts;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final ClubMailService mail;

    @Transactional(readOnly = true)
    public Map<String, Object> usage(Long id) {
        ClubRole role = require(id);
        List<Map<String, Object>> people = holders(role).stream().map(user -> person(user, role)).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", role.getId());
        body.put("code", role.getCode());
        body.put("name", role.getNameEn());
        body.put("category", role.getCategory());
        body.put("locked", rw.vikingfc.club.user.RolePolicy.locked(role.getCode()));
        body.put("people", people);
        return body;
    }

    public Map<String, Object> resolveAndDelete(Long id, Map<String, Object> body) {
        ClubRole role = require(id);
        RemovalRules.roleMayBeDeleted(role.getCode());
        List<User> holders = holders(role);
        List<Map<String, Object>> moves = maps(body.get("moves"));
        List<Map<String, Object>> deletions = maps(body.get("deletions"));
        if (moves.size() + deletions.size() != holders.size()) {
            throw BusinessException.badRequest("Every person must be moved or removed", "ROLE_IN_USE");
        }
        Map<Long, User> byId = new LinkedHashMap<>();
        for (User user : holders) {
            byId.put(user.getId(), user);
        }
        Map<Long, Integer> incoming = new LinkedHashMap<>();
        List<String> movedNames = new ArrayList<>();
        for (Map<String, Object> move : moves) {
            long userId = number(move.get("userId"));
            long targetId = number(move.get("targetRoleId"));
            User user = byId.remove(userId);
            if (user == null) {
                throw BusinessException.badRequest("That person does not have this role", "ROLE_IN_USE");
            }
            var actor = SecurityUtils.principal();
            RemovalRules.notSelf(actor == null ? null : actor.getId(), user.getId());
            ClubRole target = roles.findById(targetId).orElseThrow(() -> NotFoundException.of("Role", targetId));
            if (!target.isActive() || target.getId().equals(role.getId())) {
                throw BusinessException.badRequest("Choose another active role", "ROLE_LOCKED");
            }
            RemovalRules.sameCategory(role.getCategory(), target.getCategory());
            incoming.merge(target.getId(), 1, Integer::sum);
            movedNames.add(user.getFullName());
        }
        for (Map.Entry<Long, Integer> entry : incoming.entrySet()) {
            ClubRole target = roles.findById(entry.getKey()).orElseThrow();
            RemovalRules.seats(target.getMaxSeats(), users.countByRoleCodeAndDeletedAtIsNull(target.getCode()), entry.getValue());
        }
        List<String> deletedNames = new ArrayList<>();
        for (Map<String, Object> deletion : deletions) {
            long userId = number(deletion.get("userId"));
            User user = byId.remove(userId);
            if (user == null) {
                throw BusinessException.badRequest("That person does not have this role", "ROLE_IN_USE");
            }
            accounts.remove(user, text(deletion.get("reasonCode")), text(deletion.get("reason")), text(deletion.get("confirmName")), false);
            deletedNames.add(user.getFullName());
        }
        if (!byId.isEmpty()) {
            throw BusinessException.badRequest("Every person must be moved or removed", "ROLE_IN_USE");
        }
        for (Map<String, Object> move : moves) {
            User user = users.findById(number(move.get("userId"))).orElseThrow();
            ClubRole target = roles.findById(number(move.get("targetRoleId"))).orElseThrow();
            String from = role.getNameEn();
            applyRole(user, target);
            tell(user, from, target.getNameEn());
        }
        roles.delete(role);
        auditService.record("ROLE_RESOLVE_DELETE", "ClubRole", id, role.getCode(),
                "moved=" + movedNames.size() + " deleted=" + deletedNames.size(), null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moved", movedNames.size());
        result.put("deleted", deletedNames.size());
        return result;
    }

    private void tell(User user, String from, String to) {
        String language = user.getPreferredLanguage() == null ? "en" : user.getPreferredLanguage();
        String when = ZonedDateTime.now(KIGALI).format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"));
        String subject = switch (language) {
            case "fr" -> "Votre rôle a changé";
            case "rw" -> "Uruhare rwawe rwahindutse";
            default -> "Your role changed";
        };
        String sentence = switch (language) {
            case "fr" -> "Votre rôle est passé de " + from + " à " + to + ".";
            case "rw" -> "Uruhare rwawe rwavuye kuri " + from + " rugera kuri " + to + ".";
            default -> "Your role changed from " + from + " to " + to + ".";
        };
        notifications.notify(user, "ROLE_CHANGED", subject, sentence, "/admin");
        mail.enqueue(user.getEmail(), subject, mail.layout(subject, "<p>" + sentence + "</p><p>" + when + " · Kigali</p>"),
                "role-changed", language, "ROLE_CHANGED", String.valueOf(user.getId()));
    }

    private void applyRole(User user, ClubRole role) {
        user.setRoleCode(role.getCode());
        try {
            user.setRole(Role.valueOf(role.getCode()));
        } catch (IllegalArgumentException ex) {
            user.setRole(Role.CUSTOM);
        }
    }

    private List<User> holders(ClubRole role) {
        return users.findByDeletedAtIsNullOrderByFullNameAsc().stream()
                .filter(user -> role.getCode().equals(codeOf(user)))
                .toList();
    }

    private Map<String, Object> person(User user, ClubRole role) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.getId());
        row.put("name", user.getFullName());
        row.put("email", user.getEmail());
        row.put("photoUrl", user.getPhotoUrl());
        row.put("status", user.isActive() ? "ACTIVE" : "SUSPENDED");
        String type = "PLAYER".equalsIgnoreCase(role.getCategory()) ? "player"
                : "ADMINISTRATION".equalsIgnoreCase(role.getCategory()) ? "admin" : "staff";
        Player linked = players.findFirstByUserId(user.getId()).orElse(null);
        if (linked != null) {
            type = "player";
        }
        row.put("type", type);
        return row;
    }

    private ClubRole require(Long id) {
        return roles.findById(id).orElseThrow(() -> NotFoundException.of("Role", id));
    }

    private static String codeOf(User user) {
        return user.getRoleCode() == null || user.getRoleCode().isBlank() ? user.getRole().name() : user.getRoleCode();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rows.add((Map<String, Object>) map);
            }
        }
        return rows;
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest("A person or role is missing", "VALIDATION");
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

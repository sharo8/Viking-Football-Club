package rw.vikingfc.club.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.LabelText;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.staff.StaffMemberRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleDeskService {

    private final ClubRoleRepository roles;
    private final UserRepository users;
    private final StaffMemberRepository staff;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return roles.findAll().stream().map(this::view).toList();
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String name = text(body.get("name"));
        if (name.isBlank()) {
            name = text(body.get("nameEn"));
        }
        if (name.isBlank()) {
            throw BusinessException.badRequest("Name is required", "VALIDATION");
        }
        String code = text(body.get("code"));
        if (code.isBlank()) {
            code = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
        }
        if (roles.findByCode(code).isPresent()) {
            throw BusinessException.conflict("That role code is already used", "ROLE_CODE_TAKEN", null);
        }
        ClubRole role = new ClubRole();
        role.setCode(code);
        role.setSystemRole(false);
        apply(role, body, true);
        roles.save(role);
        auditService.record("ROLE_CREATE", "ClubRole", role.getId(), null, role.getCode(), null);
        return view(role);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        ClubRole role = require(id);
        if (RolePolicy.locked(role.getCode())) {
            throw BusinessException.badRequest("This role is protected", "ROLE_LOCKED");
        }
        apply(role, body, false);
        if ("PLAYER".equals(role.getCode())) {
            role.setOtpRequired(false);
            role.setCategory("PLAYER");
        }
        auditService.record("ROLE_UPDATE", "ClubRole", role.getId(), null, role.getCode(), null);
        return view(role);
    }

    public void delete(Long id) {
        ClubRole role = require(id);
        if (RolePolicy.locked(role.getCode())) {
            throw BusinessException.badRequest("This role is protected", "ROLE_LOCKED");
        }
        long used = users.countByRoleCodeAndDeletedAtIsNull(role.getCode());
        if (used > 0) {
            throw BusinessException.conflict("Reassign " + used + " users before deleting this role", "ROLE_IN_USE", Map.of("users", used));
        }
        roles.delete(role);
        auditService.record("ROLE_DELETE", "ClubRole", id, role.getCode(), null, null);
    }

    public void reassignAndDelete(Long id, String targetCode) {
        ClubRole role = require(id);
        ClubRole target = roles.findByCode(targetCode).orElseThrow(() -> NotFoundException.of("Role", targetCode));
        if (RolePolicy.locked(role.getCode())) {
            throw BusinessException.badRequest("This role is protected", "ROLE_LOCKED");
        }
        for (User user : users.findByDeletedAtIsNullOrderByFullNameAsc()) {
            if (role.getCode().equals(user.getRoleCode())) {
                applyRole(user, target);
            }
        }
        roles.delete(role);
        auditService.record("ROLE_REASSIGN_DELETE", "ClubRole", id, role.getCode(), target.getCode(), null);
    }

    public Map<String, Object> assign(Long id, Long userId) {
        ClubRole role = require(id);
        User user = users.findById(userId).filter(row -> row.getDeletedAt() == null)
                .orElseThrow(() -> NotFoundException.of("User", userId));
        if (role.getMaxSeats() != null) {
            long used = users.countByRoleCodeAndDeletedAtIsNull(role.getCode());
            if (used >= role.getMaxSeats() && !role.getCode().equals(user.getRoleCode())) {
                throw BusinessException.conflict("That role has no seats left", "ROLE_SEATS_FULL", null);
            }
        }
        applyRole(user, role);
        auditService.record("ROLE_ASSIGN", "User", user.getId(), null, role.getCode(), null);
        return view(role);
    }

    @Transactional(readOnly = true)
    public List<List<String>> table(String q, String lang) {
        String needle = q == null ? "" : q.toLowerCase();
        return list().stream().filter(row -> String.valueOf(row.get("name")).toLowerCase().contains(needle)
                || String.valueOf(row.get("nameEn")).toLowerCase().contains(needle)
                || String.valueOf(row.get("nameFr")).toLowerCase().contains(needle)
                || String.valueOf(row.get("nameRw")).toLowerCase().contains(needle)
                || needle.isBlank()).map(row -> List.of(
                exportName(row, lang),
                String.valueOf(row.get("category")),
                Boolean.TRUE.equals(row.get("otpRequired")) ? "OTP" : "",
                String.valueOf(row.get("users"))
        )).toList();
    }

    public List<Map<String, Object>> cleanup() {
        return staff.findByActiveTrueOrderByFullNameAsc().stream()
                .filter(member -> member.getEmail() != null)
                .filter(member -> member.getListedIn() == null || "STAFF".equalsIgnoreCase(member.getListedIn()))
                .filter(member -> users.findByEmailIgnoreCaseAndDeletedAtIsNull(member.getEmail())
                        .map(user -> {
                            String code = user.getRoleCode() == null || user.getRoleCode().isBlank() ? user.getRole().name() : user.getRoleCode();
                            return "ADMINISTRATION".equals(RolePolicy.category(code));
                        })
                        .orElse(false))
                .map(member -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", member.getFullName());
                    row.put("email", member.getEmail());
                    row.put("role", member.getRoleName());
                    row.put("note", "Same email exists as a login and as staff. Nothing was deleted.");
                    return row;
                })
                .toList();
    }

    private void applyRole(User user, ClubRole role) {
        user.setRoleCode(role.getCode());
        try {
            user.setRole(Role.valueOf(role.getCode()));
        } catch (IllegalArgumentException ex) {
            user.setRole(Role.CUSTOM);
        }
    }

    private void apply(ClubRole role, Map<String, Object> body, boolean creating) {
        String category = text(body.get("category"));
        if (!category.isBlank()) {
            role.setCategory(category.toUpperCase(Locale.ROOT));
        } else if (creating) {
            role.setCategory("STAFF");
        }
        if (role.isSystemRole() && !creating) {
            // Seeded roles are translated in the language files, not by this form.
        } else {
            String source = text(body.get("sourceLang"));
            if (!source.isBlank()) {
                role.setSourceLang(source.toLowerCase(Locale.ROOT));
            } else if (creating) {
                role.setSourceLang("en");
            }
            String name = text(body.get("name"));
            if (name.isBlank()) {
                name = text(body.get("nameEn"));
            }
            if (body.get("nameEn") != null) role.setNameEn(text(body.get("nameEn")));
            if (body.get("nameFr") != null) role.setNameFr(text(body.get("nameFr")));
            if (body.get("nameRw") != null) role.setNameRw(text(body.get("nameRw")));
            if (!name.isBlank()) {
                write(role, role.getSourceLang(), name);
            }
            if (role.getNameEn() == null) role.setNameEn("");
            if (role.getNameFr() == null) role.setNameFr("");
            if (role.getNameRw() == null) role.setNameRw("");
        }
        if (body.get("description") != null) role.setDescription(text(body.get("description")));
        if (body.get("color") != null) role.setColor(text(body.get("color")));
        if (body.get("otpRequired") instanceof Boolean flag) role.setOtpRequired(flag);
        if (body.get("active") instanceof Boolean flag) role.setActive(flag);
        if (body.get("permissions") != null) role.setPermissionsJson(String.valueOf(body.get("permissions")));
        if (body.get("maxSeats") instanceof Number number) role.setMaxSeats(number.intValue());
    }

    private Map<String, Object> view(ClubRole role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", role.getId());
        body.put("code", role.getCode());
        body.put("category", role.getCategory());
        String source = role.getSourceLang() == null ? "en" : role.getSourceLang();
        String main = LabelText.pick(source, source, "", role.getNameEn(), role.getNameFr(), role.getNameRw());
        body.put("sourceLang", source);
        body.put("name", main);
        body.put("nameEn", role.getNameEn());
        body.put("nameFr", role.getNameFr());
        body.put("nameRw", role.getNameRw());
        Map<String, String> system = role.isSystemRole() ? RoleCopy.of(role.getCode()) : null;
        body.put("label", system != null ? system : Map.of(
                "en", LabelText.pick("en", source, main, role.getNameEn(), role.getNameFr(), role.getNameRw()),
                "fr", LabelText.pick("fr", source, main, role.getNameEn(), role.getNameFr(), role.getNameRw()),
                "rw", LabelText.pick("rw", source, main, role.getNameEn(), role.getNameFr(), role.getNameRw())
        ));
        body.put("description", role.getDescription());
        body.put("color", role.getColor());
        body.put("otpRequired", role.isOtpRequired());
        body.put("systemRole", role.isSystemRole());
        body.put("locked", RolePolicy.locked(role.getCode()));
        body.put("maxSeats", role.getMaxSeats());
        body.put("active", role.isActive());
        body.put("permissions", role.getPermissionsJson());
        body.put("users", users.countByRoleCodeAndDeletedAtIsNull(role.getCode()));
        return body;
    }

    private ClubRole require(Long id) {
        return roles.findById(id).orElseThrow(() -> NotFoundException.of("Role", id));
    }

    private static String exportName(Map<String, Object> row, String lang) {
        Object labels = row.get("label");
        if (labels instanceof Map<?, ?> map && map.get(lang) != null) {
            return String.valueOf(map.get(lang));
        }
        return String.valueOf(row.get("name"));
    }

    private static void write(ClubRole role, String lang, String value) {
        switch (lang) {
            case "fr" -> role.setNameFr(value);
            case "rw" -> role.setNameRw(value);
            default -> role.setNameEn(value);
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

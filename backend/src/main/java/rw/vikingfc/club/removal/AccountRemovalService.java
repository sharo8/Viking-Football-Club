package rw.vikingfc.club.removal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.player.PlayerRepository;
import rw.vikingfc.club.staff.StaffMember;
import rw.vikingfc.club.staff.StaffMemberRepository;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountRemovalService {

    private final UserRepository users;
    private final StaffMemberRepository staff;
    private final PlayerRepository players;
    private final DeletedUserRepository deletedUsers;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final Clock clock;

    public Map<String, Object> removeUser(Long id, String reasonCode, String reasonText, String confirmName) {
        User user = users.findById(id).filter(row -> row.getDeletedAt() == null)
                .orElseThrow(() -> NotFoundException.of("User", id));
        return remove(user, reasonCode, reasonText, confirmName, true);
    }

    public Map<String, Object> removeStaff(Long id, String reasonCode, String reasonText, String confirmName) {
        StaffMember member = staff.findByIdAndActiveTrue(id).orElseThrow(() -> NotFoundException.of("Staff member", id));
        User linked = member.getEmail() == null ? null
                : users.findByEmailIgnoreCaseAndDeletedAtIsNull(member.getEmail()).orElse(null);
        if (linked != null) {
            return remove(linked, reasonCode, reasonText, confirmName, true);
        }
        RemovalRules.reason(reasonCode, reasonText);
        RemovalRules.confirmName(member.getFullName(), confirmName);
        member.setActive(false);
        member.setDeletedAt(Instant.now(clock));
        member.setStatus("ARCHIVED");
        archiveRecord(null, member.getFullName(), member.getEmail(), member.getRoleName(), reasonCode, reasonText, false);
        auditService.record("STAFF_ARCHIVE", "StaffMember", id, member.getFullName(), reasonText, null);
        notifyExecutives("Account removed", member.getFullName() + " was archived. " + reasonText);
        return Map.of("archived", true, "name", member.getFullName());
    }

    public Map<String, Object> remove(User user, String reasonCode, String reasonText, String confirmName, boolean requireName) {
        RemovalRules.reason(reasonCode, reasonText);
        if (requireName) {
            RemovalRules.confirmName(user.getFullName(), confirmName);
        } else if (confirmName != null && !confirmName.isBlank()) {
            RemovalRules.confirmName(user.getFullName(), confirmName);
        }
        var actor = SecurityUtils.principal();
        RemovalRules.notSelf(actor == null ? null : actor.getId(), user.getId());
        String code = user.getRoleCode() == null || user.getRoleCode().isBlank() ? user.getRole().name() : user.getRoleCode();
        RemovalRules.notLastExecutive(code, users.countByRoleCodeAndDeletedAtIsNull(code));
        boolean history = user.getLastLoginAt() != null
                || staff.findFirstByEmailIgnoreCaseAndDeletedAtIsNull(user.getEmail()).isPresent()
                || players.findFirstByUserId(user.getId()).isPresent();
        Instant now = Instant.now(clock);
        staff.findFirstByEmailIgnoreCaseAndDeletedAtIsNull(user.getEmail()).ifPresent(member -> {
            member.setActive(false);
            member.setDeletedAt(now);
            member.setStatus("ARCHIVED");
        });
        boolean hard = !history;
        if (hard) {
            users.delete(user);
        } else {
            user.setActive(false);
            user.setDeletedAt(now);
        }
        archiveRecord(hard ? null : user.getId(), user.getFullName(), user.getEmail(), code, reasonCode, reasonText, hard);
        auditService.record(hard ? "USER_DELETE" : "USER_ARCHIVE", "User", user.getId(), user.getFullName(), reasonText, null);
        notifyExecutives(hard ? "Account deleted" : "Account archived", user.getFullName() + " — " + reasonText);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("archived", !hard);
        body.put("name", user.getFullName());
        return body;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> removed() {
        return deletedUsers.findByHardDeletedFalseAndRestoreUntilAfterOrderByCreatedAtDesc(Instant.now(clock)).stream().map(row -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", row.getId());
            body.put("userId", row.getUserId());
            body.put("name", row.getFullName());
            body.put("email", row.getEmail());
            body.put("reason", row.getReasonText());
            body.put("reasonCode", row.getReasonCode());
            body.put("until", row.getRestoreUntil());
            return body;
        }).toList();
    }

    public void archiveLinked(User user, String reason) {
        Instant now = Instant.now(clock);
        user.setActive(false);
        user.setDeletedAt(now);
        String code = user.getRoleCode() == null || user.getRoleCode().isBlank() ? user.getRole().name() : user.getRoleCode();
        archiveRecord(user.getId(), user.getFullName(), user.getEmail(), code, "OTHER", reason, false);
        auditService.record("USER_ARCHIVE", "User", user.getId(), user.getFullName(), reason, null);
    }

    public Map<String, Object> restore(Long userId) {
        if (!SecurityUtils.isExecutive()) {
            throw BusinessException.forbidden("Only an executive can restore an account");
        }
        DeletedUser record = deletedUsers.findFirstByUserIdAndHardDeletedFalseOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> NotFoundException.of("Removed account", userId));
        if (record.getRestoreUntil() == null || record.getRestoreUntil().isBefore(Instant.now(clock))) {
            throw BusinessException.badRequest("The 30 day restore window has ended", "RESTORE_CLOSED");
        }
        User user = users.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        user.setDeletedAt(null);
        user.setActive(true);
        record.setRestoreUntil(Instant.now(clock));
        auditService.record("USER_RESTORE", "User", userId, null, user.getFullName(), null);
        return Map.of("id", userId, "name", user.getFullName());
    }

    private void archiveRecord(Long userId, String name, String email, String role, String reasonCode, String reasonText, boolean hard) {
        DeletedUser row = new DeletedUser();
        row.setUserId(userId);
        row.setFullName(name);
        row.setEmail(email);
        row.setRoleCode(role);
        row.setReasonCode(reasonCode == null ? "OTHER" : reasonCode);
        row.setReasonText(reasonText == null ? "" : reasonText.trim());
        row.setSnapshotJson(name + " <" + email + "> " + role);
        row.setDeletedBy(SecurityUtils.email());
        row.setHardDeleted(hard);
        row.setRestoreUntil(hard ? null : Instant.now(clock).plus(30, ChronoUnit.DAYS));
        deletedUsers.save(row);
    }

    private void notifyExecutives(String title, String body) {
        notifications.notifyRoles(List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER), "ACCOUNT_REMOVED", title, body, "/admin/audit");
    }
}

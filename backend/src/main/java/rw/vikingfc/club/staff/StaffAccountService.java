package rw.vikingfc.club.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.auth.RefreshTokenRepository;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.marketing.NotificationService;
import rw.vikingfc.club.removal.RemovalRules;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.RolePolicy;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffAccountService {

    private final StaffMemberRepository staffRepository;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ClubMailService mail;
    private final NotificationService notifications;
    private final AuditService audit;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public User open(StaffMember incoming, String email, boolean sendInvitation, String language) {
        StaffMember staff = staffRepository.findById(incoming.getId())
                .orElseThrow(() -> NotFoundException.of("Staff member", incoming.getId()));
        StaffAccountRules.notArchived(staff.isActive() && staff.getDeletedAt() == null);
        if (staff.getUser() != null && staff.getUser().getDeletedAt() == null) {
            throw BusinessException.conflict("This person already has an account", "ACCOUNT_EXISTS", null);
        }
        String code = RolePolicy.codeOf(staff.getRoleName());
        StaffAccountRules.staffCategory(code);
        Role role = roleOf(code);
        StaffAccountRules.email(email);
        String address = email.trim().toLowerCase();
        StaffAccountRules.unique(users.findByEmailIgnoreCase(address).isPresent());
        boolean already = staff.getUser() != null && code.equals(staff.getUser().getRoleCode());
        StaffAccountRules.headCoachSeat(code, users.countByRoleAndActiveTrueAndDeletedAtIsNull(role), already);
        String temporary = "Viking@" + (100000 + random.nextInt(900000));
        User user = new User();
        user.setEmail(address);
        user.setFullName(staff.getFullName());
        user.setRole(role);
        user.setRoleCode(code);
        user.setPhone(staff.getPhone());
        user.setPhotoUrl(staff.getPhotoUrl());
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setPreferredLanguage(language == null || language.isBlank() ? "en" : language);
        user.setFailedAttempts(0);
        users.save(user);
        staff.setUser(user);
        audit.record("STAFF_ACCOUNT_CREATE", "StaffMember", staff.getId(), null, address, null);
        notifications.notify(user, "STAFF_ACCOUNT", "Your Viking FC account", "Sign in and change the temporary password.", "/login");
        if (sendInvitation) {
            invite(user, staff.getRoleName(), temporary);
        }
        return user;
    }

    public void resend(Long staffId) {
        User user = requireUser(staffId);
        String temporary = reset(user);
        audit.record("STAFF_INVITE_RESEND", "User", user.getId(), null, user.getEmail(), null);
        invite(user, load(staffId).getRoleName(), temporary);
        notifications.notify(user, "STAFF_ACCOUNT", "Invitation sent again", "Use the new temporary password and change it at the first login.", "/login");
    }

    public void resetPassword(Long staffId) {
        User user = requireUser(staffId);
        String temporary = reset(user);
        audit.record("STAFF_PASSWORD_RESET", "User", user.getId(), null, user.getEmail(), null);
        invite(user, load(staffId).getRoleName(), temporary);
        notifications.notify(user, "STAFF_ACCOUNT", "Password reset", "Use the temporary password and change it at the next login.", "/login");
    }

    public void suspend(Long staffId) {
        User user = requireUser(staffId);
        RemovalRules.notSelf(actorId(), user.getId());
        user.setActive(false);
        refreshTokens.revokeAllForUser(user.getId());
        audit.record("STAFF_ACCOUNT_SUSPEND", "User", user.getId(), "ACTIVE", "SUSPENDED", null);
        notifications.notify(user, "STAFF_ACCOUNT", "Account suspended", "Contact the administration to activate your account.", "/login");
    }

    public void activate(Long staffId) {
        User user = requireUser(staffId);
        user.setActive(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        audit.record("STAFF_ACCOUNT_ACTIVATE", "User", user.getId(), "SUSPENDED", "ACTIVE", null);
        notifications.notify(user, "STAFF_ACCOUNT", "Account activated", "You can sign in again.", "/login");
    }

    public void remove(Long staffId, String reasonCode, String reasonText) {
        StaffAccountRules.reason(reasonCode, reasonText);
        StaffMember staff = load(staffId);
        User user = staff.getUser();
        if (user == null || user.getDeletedAt() != null) {
            throw BusinessException.badRequest("This person has no account", "NO_ACCOUNT");
        }
        RemovalRules.notSelf(actorId(), user.getId());
        user.setActive(false);
        user.setDeletedAt(Instant.now(clock));
        String rewritten = "removed-" + user.getId() + "-" + user.getEmail();
        user.setEmail(rewritten.length() > 180 ? rewritten.substring(0, 180) : rewritten);
        refreshTokens.revokeAllForUser(user.getId());
        staff.setUser(null);
        audit.record("STAFF_ACCOUNT_REMOVE", "User", user.getId(), user.getFullName(), reasonText, null);
    }

    private String reset(User user) {
        String temporary = "Viking@" + (100000 + random.nextInt(900000));
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        refreshTokens.revokeAllForUser(user.getId());
        return temporary;
    }

    private void invite(User user, String role, String temporary) {
        String lang = user.getPreferredLanguage() == null ? "en" : user.getPreferredLanguage();
        boolean otp = RolePolicy.otpRequired(user.getRoleCode());
        String subject = switch (lang) {
            case "fr" -> "Votre compte Viking FC";
            case "rw" -> "Konti yawe ya Viking FC";
            default -> "Your Viking FC account";
        };
        String otpLine = otp
                ? switch (lang) {
                    case "fr" -> "Un code est demandé à chaque connexion.";
                    case "rw" -> "Kode isabwa buri gihe winjira.";
                    default -> "A code is required at every login.";
                }
                : switch (lang) {
                    case "fr" -> "Un code à la connexion est facultatif.";
                    case "rw" -> "Kode yo kwinjira si itegeko.";
                    default -> "A login code is optional.";
                };
        String body = switch (lang) {
            case "fr" -> "<p>Bonjour " + user.getFullName() + ",</p><p>Votre rôle : " + role + ".</p><p>Mot de passe temporaire : <strong>" + temporary + "</strong></p><p>Connectez-vous et changez-le une seule fois, à la première connexion.</p><p>" + otpLine + "</p>";
            case "rw" -> "<p>Muraho " + user.getFullName() + ",</p><p>Uruhare rwawe: " + role + ".</p><p>Ijambobanga ry'igihe gito: <strong>" + temporary + "</strong></p><p>Injira uhitemo irindi rimwe gusa, ubwa mbere winjira.</p><p>" + otpLine + "</p>";
            default -> "<p>Hello " + user.getFullName() + ",</p><p>Your role: " + role + ".</p><p>Temporary password: <strong>" + temporary + "</strong></p><p>Sign in and change it once, at the first login.</p><p>" + otpLine + "</p>";
        };
        var queued = mail.enqueue(user.getEmail(), subject, mail.layout(subject, body), "staff-invite", lang, "STAFF_ACCOUNT", String.valueOf(user.getId()));
        mail.dispatch(queued.getId());
    }

    private StaffMember load(Long id) {
        return staffRepository.findById(id).orElseThrow(() -> NotFoundException.of("Staff member", id));
    }

    private User requireUser(Long staffId) {
        StaffMember staff = load(staffId);
        User user = staff.getUser();
        if (user == null || user.getDeletedAt() != null) {
            throw BusinessException.badRequest("This person has no account", "NO_ACCOUNT");
        }
        return user;
    }

    private static Role roleOf(String code) {
        try {
            return Role.valueOf(code);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest("Staff accounts use a staff role", "ROLE_CATEGORY_MISMATCH");
        }
    }

    private static Long actorId() {
        var actor = SecurityUtils.principal();
        return actor == null ? null : actor.getId();
    }
}

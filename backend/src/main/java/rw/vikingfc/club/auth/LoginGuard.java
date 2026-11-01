package rw.vikingfc.club.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginGuard {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AppProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        boolean locked = attempts >= properties.security().maxFailedAttempts();
        if (locked) {
            user.setLockedUntil(Instant.now(clock).plus(properties.security().lockMinutes(), ChronoUnit.MINUTES));
            user.setFailedAttempts(0);
        }
        String roleName = user.getRoleCode() == null || user.getRoleCode().isBlank() ? user.getRole().name() : user.getRoleCode();
        auditService.recordFor(user.getEmail(), user.getFullName(), roleName, locked ? "LOGIN_LOCKOUT" : "LOGIN_FAILED", "User", user.getId(), null, null);
        log.warn("Login rejected for {}: {}", user.getEmail(), locked ? "LOCKED" : "BAD_PASSWORD");
    }
}

package rw.vikingfc.club.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.security.UserPrincipal;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SessionTrackingService {

    private static final long ACTIVE_GAP_SECONDS = 120;
    private static final long IDLE_BREAK_SECONDS = 900;

    private final UserSessionRepository sessions;

    @Transactional
    public void heartbeat(String ip, String userAgent) {
        UserPrincipal principal = SecurityUtils.principal();
        if (principal == null) return;
        Instant now = Instant.now();
        UserSession open = sessions.findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(principal.getId()).orElse(null);
        if (open == null) {
            sessions.save(start(principal, now, ip, userAgent));
            return;
        }
        long gap = Duration.between(open.getLastSeenAt(), now).getSeconds();
        if (gap > IDLE_BREAK_SECONDS) {
            open.setEndedAt(open.getLastSeenAt());
            open.setEndReason("TIMEOUT");
            sessions.save(open);
            sessions.save(start(principal, now, ip, userAgent));
            return;
        }
        if (gap > 0 && gap <= ACTIVE_GAP_SECONDS) {
            open.setActiveSeconds(open.getActiveSeconds() + (int) gap);
        }
        open.setLastSeenAt(now);
        sessions.save(open);
    }

    @Transactional
    public void end(String reason) {
        UserPrincipal principal = SecurityUtils.principal();
        if (principal == null) return;
        UserSession open = sessions.findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(principal.getId()).orElse(null);
        if (open == null) return;
        Instant now = Instant.now();
        long gap = Duration.between(open.getLastSeenAt(), now).getSeconds();
        if (gap > 0 && gap <= ACTIVE_GAP_SECONDS) {
            open.setActiveSeconds(open.getActiveSeconds() + (int) gap);
        }
        open.setLastSeenAt(now);
        open.setEndedAt(now);
        open.setEndReason(reason == null || reason.isBlank() ? "LOGOUT" : reason);
        sessions.save(open);
    }

    private static UserSession start(UserPrincipal principal, Instant now, String ip, String userAgent) {
        UserSession session = new UserSession();
        session.setUserId(principal.getId());
        session.setRoleSnapshot(principal.getRole().name());
        session.setStartedAt(now);
        session.setLastSeenAt(now);
        session.setIpAddress(ip);
        String agent = userAgent == null ? "" : userAgent;
        session.setUserAgent(agent.length() > 240 ? agent.substring(0, 240) : agent);
        session.setActiveSeconds(0);
        return session;
    }
}

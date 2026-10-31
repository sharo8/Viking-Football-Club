package rw.vikingfc.club.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.security.JwtService;
import rw.vikingfc.club.security.LoginRateLimiter;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;
import rw.vikingfc.club.user.UserService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;
    private final AppProperties properties;
    private final AuditService auditService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request, String ip) {
        loginRateLimiter.check(ip == null ? "unknown" : ip);
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isActive()) {
            throw new BadCredentialsException("This account is disabled");
        }
        Instant now = Instant.now(clock);
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BusinessException(HttpStatus.LOCKED,
                    "Account locked after too many failed attempts. Try again in 15 minutes.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= properties.security().maxFailedAttempts()) {
                user.setLockedUntil(now.plus(properties.security().lockMinutes(), ChronoUnit.MINUTES));
                user.setFailedAttempts(0);
            }
            throw new BadCredentialsException("Invalid email or password");
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        return issue(user);
    }

    public AuthDtos.TokenResponse refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = current.getUser();
        if (!user.isActive()) {
            throw new BadCredentialsException("This account is disabled");
        }
        current.setRevoked(true);
        AuthDtos.TokenResponse issued = issue(user);
        current.setReplacedByHash(hash(issued.refreshToken()));
        return issued;
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> token.setRevoked(true));
    }

    public AuthDtos.TokenResponse changePassword(AuthDtos.ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Password change rejected for {}: INVALID_CURRENT_PASSWORD", user.getEmail());
            throw BusinessException.badRequest("Current password is incorrect", "INVALID_CURRENT_PASSWORD");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            log.warn("Password change rejected for {}: PASSWORD_REUSE", user.getEmail());
            throw BusinessException.badRequest(
                    "Choose a password that is different from the current one", "PASSWORD_REUSE");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(Instant.now(clock));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.saveAndFlush(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.record("PASSWORD_CHANGE", "User", user.getId(), null, "password-changed", null);
        log.info("Password changed for {}", user.getEmail());
        return issue(user);
    }

    public void devResetTemporaryPassword(String email) {
        if (email == null || email.isBlank()) {
            throw BusinessException.badRequest("Email is required", "VALIDATION");
        }
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() -> NotFoundException.of("User", email));
        user.setPasswordHash(passwordEncoder.encode(UserService.TEMP_PASSWORD));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(null);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.warn("DEV reset temporary password for {}", user.getEmail());
    }

    public AuthDtos.ForgotResponse forgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() -> NotFoundException.of("User", email));
        String raw = randomToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now(clock).plus(30, ChronoUnit.MINUTES));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);
        return new AuthDtos.ForgotResponse(
                "Reset token created. It expires in 30 minutes. Wire this to email before a public deployment.",
                raw);
    }

    public void resetWithToken(AuthDtos.ResetWithTokenRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedFalse(hash(request.token()))
                .orElseThrow(() -> BusinessException.unprocessable("Reset token is invalid"));
        if (token.getExpiresAt().isBefore(Instant.now(clock))) {
            throw BusinessException.unprocessable("Reset token has expired");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(Instant.now(clock));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        token.setUsed(true);
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public AuthDtos.Me me() {
        User user = currentUser();
        return new AuthDtos.Me(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getRole().isExecutive(),
                user.isMustChangePassword()
        );
    }

    private AuthDtos.TokenResponse issue(User user) {
        String refresh = randomToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(refresh));
        token.setExpiresAt(Instant.now(clock).plus(properties.jwt().refreshDays(), ChronoUnit.DAYS));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
        return new AuthDtos.TokenResponse(
                jwtService.accessToken(user),
                refresh,
                properties.jwt().accessMinutes() * 60L,
                user.isMustChangePassword(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getRole().isExecutive()
        );
    }

    private User currentUser() {
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(SecurityUtils.email())
                .orElseThrow(() -> new BadCredentialsException("Authentication required"));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

package rw.vikingfc.club.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.RwandaPhone;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.security.JwtService;
import rw.vikingfc.club.security.LoginRateLimiter;
import rw.vikingfc.club.user.ClubRoleRepository;
import rw.vikingfc.club.user.RolePolicy;
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
    private final OtpService otpService;
    private final ClubRoleRepository clubRoleRepository;
    private final UserService userService;
    private final LoginGuard loginGuard;
    private final SecureRandom random = new SecureRandom();

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request, String ip) {
        loginRateLimiter.check(ip == null ? "unknown" : ip);
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(java.util.Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).orElse(null);
        if (user == null) {
            log.warn("Login rejected for {}: USER_NOT_FOUND", email);
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            log.warn("Login rejected for {}: SUSPENDED", user.getEmail());
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "This account is inactive. Contact the administration to activate it.",
                    "ACCOUNT_INACTIVE");
        }
        Instant now = Instant.now(clock);
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Math.max(1, ChronoUnit.MINUTES.between(now, user.getLockedUntil()));
            log.warn("Login rejected for {}: LOCKED for {} minutes", user.getEmail(), minutes);
            throw new BusinessException(HttpStatus.LOCKED,
                    "This account is locked. Try again in " + minutes + " minutes.",
                    "ACCOUNT_LOCKED");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginGuard.recordFailure(user.getId());
            throw new BadCredentialsException("Invalid email or password");
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        String code = user.getRoleCode() == null || user.getRoleCode().isBlank() ? user.getRole().name() : user.getRoleCode();
        if (!user.isMustChangePassword() && requiresOtp(code)) {
            String challenge = otpService.beginLogin(user, ip);
            auditService.recordFor(user.getEmail(), user.getFullName(), code, "LOGIN_OTP_SENT", "User", user.getId(), null, null);
            return AuthDtos.LoginResponse.challenge(challenge, mask(user.getEmail()));
        }
        user.setLastLoginAt(now);
        auditService.recordFor(user.getEmail(), user.getFullName(), code, "LOGIN", "User", user.getId(), null, null);
        return AuthDtos.LoginResponse.tokens(issue(user));
    }

    public AuthDtos.LoginResponse verifyLoginOtp(String challengeId, String code) {
        User user = otpService.consumeLogin(challengeId, code == null ? "" : code.trim());
        user.setLastLoginAt(Instant.now(clock));
        auditService.recordFor(user.getEmail(), user.getFullName(), user.getRole().name(), "LOGIN_OTP_OK", "User", user.getId(), null, null);
        return AuthDtos.LoginResponse.tokens(issue(user));
    }

    public void resendLoginOtp(String challengeId, String ip) {
        otpService.resendLogin(challengeId, ip);
    }

    private boolean requiresOtp(String code) {
        if ("PLAYER".equals(code)) {
            return false;
        }
        if (RolePolicy.executive(code)) {
            return true;
        }
        return clubRoleRepository.findByCode(code).map(row -> row.isOtpRequired()).orElse(RolePolicy.otpRequired(code));
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    public AuthDtos.TokenResponse refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = current.getUser();
        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "This account is inactive. Contact the administration to activate it.",
                    "ACCOUNT_INACTIVE");
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

    public java.util.Map<String, Object> updateProfile(String rawEmail, String rawPhone) {
        User user = currentUser();
        if (rawEmail != null) {
            if (!rawEmail.trim().matches("(?i)^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw BusinessException.badRequest("Enter a valid email address.", "EMAIL_INVALID");
            }
            String email = rawEmail.trim().toLowerCase(java.util.Locale.ROOT);
            if (!email.equalsIgnoreCase(user.getEmail())) {
                userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).ifPresent(other -> {
                    throw BusinessException.conflict("That email is already used.", "EMAIL_TAKEN", null);
                });
                String previous = user.getEmail();
                user.setEmail(email);
                auditService.record("EMAIL_UPDATE", "User", user.getId(), previous, email, null);
            }
        }
        if (rawPhone != null) {
            String phone = rawPhone.trim();
            if (phone.isEmpty()) {
                String previous = user.getPhone();
                user.setPhone(null);
                if (previous != null && !previous.isBlank()) {
                    auditService.record("PHONE_UPDATE", "User", user.getId(), previous, null, null);
                }
            } else {
                RwandaPhone.require(phone);
                phone = RwandaPhone.normalize(phone);
                userRepository.findFirstByPhoneAndDeletedAtIsNull(phone).ifPresent(other -> {
                    if (!other.getId().equals(user.getId())) {
                        throw BusinessException.conflict("That phone number is already used.", "PHONE_TAKEN", null);
                    }
                });
                String previous = user.getPhone();
                user.setPhone(phone);
                if (previous == null || !previous.equals(phone)) {
                    auditService.record("PHONE_UPDATE", "User", user.getId(), previous, phone, null);
                }
            }
        }
        return profileBody(user);
    }

    public java.util.Map<String, Object> updatePhoto(org.springframework.web.multipart.MultipartFile file) {
        User user = currentUser();
        String previous = user.getPhotoUrl();
        userService.photo(user.getId(), file);
        auditService.record("PHOTO_UPDATE", "User", user.getId(), previous, user.getPhotoUrl(), null);
        return profileBody(user);
    }

    public java.util.Map<String, Object> clearPhoto() {
        User user = currentUser();
        String previous = user.getPhotoUrl();
        userService.clearPhoto(user.getId());
        auditService.record("PHOTO_UPDATE", "User", user.getId(), previous, null, null);
        return profileBody(user);
    }

    private java.util.Map<String, Object> profileBody(User user) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("email", user.getEmail());
        body.put("fullName", user.getFullName());
        body.put("phone", user.getPhone());
        body.put("photoUrl", user.getPhotoUrl());
        body.put("accessToken", jwtService.accessToken(user));
        return body;
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
                user.isMustChangePassword(),
                user.getPhone(),
                user.getPhotoUrl()
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

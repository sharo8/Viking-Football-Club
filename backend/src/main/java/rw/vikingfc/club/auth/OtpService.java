package rw.vikingfc.club.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.mail.ClubMailService;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpService {

    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String LOGIN_2FA = "LOGIN_2FA";
    public static final String EMAIL_VERIFY = "EMAIL_VERIFY";

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final ClubMailService mailService;
    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Environment environment;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public void request(String email, String purpose, String ip) {
        if (email == null || email.isBlank()) {
            return;
        }
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim()).orElse(null);
        if (user == null) {
            return;
        }
        Instant now = Instant.now(clock);
        otpCodeRepository.findByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .stream().findFirst().ifPresent(latest -> {
                    if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(now.minus(60, ChronoUnit.SECONDS))) {
                        throw BusinessException.badRequest("Wait a minute before asking for another code", "OTP_COOLDOWN");
                    }
                });
        if (otpCodeRepository.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, now.minus(1, ChronoUnit.HOURS)) >= 5) {
            throw BusinessException.badRequest("Too many codes were requested. Try again in an hour.", "OTP_RATE");
        }
        for (OtpCode open : otpCodeRepository.findByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)) {
            open.setUsedAt(now);
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpCode row = new OtpCode();
        row.setUser(user);
        row.setPurpose(purpose);
        row.setCodeHash(hash(user.getId(), purpose, code));
        row.setExpiresAt(now.plus(5, ChronoUnit.MINUTES));
        row.setAttempts(0);
        row.setMaxAttempts(5);
        row.setIpAddress(ip);
        otpCodeRepository.save(row);
        String language = user.getPreferredLanguage() == null ? "en" : user.getPreferredLanguage();
        String subject = switch (language) {
            case "fr" -> "Votre code Viking FC";
            case "rw" -> "Kode yawe ya Viking FC";
            default -> "Your Viking FC code";
        };
        String html = mailService.layout(subject, "<p style=\"font-size:28px;letter-spacing:.3em;font-weight:700\">" + code + "</p><p>This code expires in 5 minutes.</p>");
        var queued = mailService.enqueue(user.getEmail(), subject, html, "otp", language, "OTP", purpose);
        mailService.dispatch(queued.getId());
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            log.warn("DEV OTP {} for {} purpose {}", code, user.getEmail(), purpose);
        }
    }

    public void verify(String email, String purpose, String code) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() -> BusinessException.badRequest("That code is not valid", "OTP_INVALID"));
        OtpCode row = otpCodeRepository.findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> BusinessException.badRequest("That code is not valid", "OTP_INVALID"));
        Instant now = Instant.now(clock);
        if (row.getExpiresAt().isBefore(now) || row.getAttempts() >= row.getMaxAttempts()) {
            throw BusinessException.badRequest("That code has expired", "OTP_EXPIRED");
        }
        if (!MessageDigest.isEqual(row.getCodeHash().getBytes(StandardCharsets.UTF_8), hash(user.getId(), purpose, code).getBytes(StandardCharsets.UTF_8))) {
            row.setAttempts(row.getAttempts() + 1);
            if (row.getAttempts() >= row.getMaxAttempts()) {
                row.setUsedAt(now);
            }
            throw BusinessException.badRequest("That code is not valid", "OTP_INVALID");
        }
        row.setUsedAt(now);
    }

    public void resetPassword(String email, String code, String newPassword) {
        if (newPassword == null || !newPassword.matches(AuthDtos.PASSWORD_RULE)) {
            throw BusinessException.badRequest("Use 8+ characters with upper, lower, digit and symbol", "WEAK_PASSWORD");
        }
        verify(email, PASSWORD_RESET, code);
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() -> BusinessException.badRequest("That code is not valid", "OTP_INVALID"));
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw BusinessException.badRequest("Choose a password that is different from the current one", "PASSWORD_REUSE");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(Instant.now(clock));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setEmailVerified(true);
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset by OTP for {}", user.getEmail());
    }

    private String hash(Long userId, String purpose, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((purpose + ":" + userId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}

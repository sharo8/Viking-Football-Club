package rw.vikingfc.club.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import rw.vikingfc.club.user.Role;

public final class AuthDtos {

    private AuthDtos() {
    }

    public static final String PASSWORD_RULE =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
        public LoginRequest {
            if (email != null) {
                email = email.trim().toLowerCase(java.util.Locale.ROOT);
            }
        }
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = "Use 8+ characters with upper, lower, digit and symbol")
            String newPassword
    ) {
    }

    public record ForgotRequest(@NotBlank @Email String email) {
    }

    public record ResetWithTokenRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = "Use 8+ characters with upper, lower, digit and symbol")
            String newPassword
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long accessExpiresInSeconds,
            boolean mustChangePassword,
            String email,
            String fullName,
            Role role,
            boolean executive
    ) {
    }

    public record Me(
            Long id,
            String email,
            String fullName,
            Role role,
            boolean executive,
            boolean mustChangePassword,
            String phone,
            String photoUrl
    ) {
    }

    public record ForgotResponse(String message, String resetToken) {
    }

    public record LoginResponse(
            boolean otpRequired,
            String challengeId,
            String maskedEmail,
            Integer expiresIn,
            String accessToken,
            String refreshToken,
            Long accessExpiresInSeconds,
            Boolean mustChangePassword,
            String email,
            String fullName,
            Role role,
            Boolean executive
    ) {
        public static LoginResponse challenge(String challengeId, String maskedEmail) {
            return new LoginResponse(true, challengeId, maskedEmail, 300, null, null, null, null, null, null, null, null);
        }

        public static LoginResponse tokens(TokenResponse tokens) {
            return new LoginResponse(false, null, null, null,
                    tokens.accessToken(), tokens.refreshToken(), tokens.accessExpiresInSeconds(),
                    tokens.mustChangePassword(), tokens.email(), tokens.fullName(), tokens.role(), tokens.executive());
        }
    }
}

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

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
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
            boolean mustChangePassword
    ) {
    }

    public record ForgotResponse(String message, String resetToken) {
    }
}

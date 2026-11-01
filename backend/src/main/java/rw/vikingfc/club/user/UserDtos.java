package rw.vikingfc.club.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateRequest(
            @NotBlank @Email String email,
            @NotBlank String fullName,
            @NotNull Role role,
            String phone
    ) {
    }

    public record UpdateRequest(
            @NotBlank String fullName,
            @NotNull Role role,
            String phone,
            @NotNull Boolean active
    ) {
    }

    public record View(
            Long id,
            String email,
            String fullName,
            Role role,
            boolean executive,
            String phone,
            boolean active,
            boolean mustChangePassword,
            java.time.Instant lastLoginAt,
            java.time.Instant createdAt,
            String photoUrl
    ) {
    }

    public record Credentials(Long id, String email, String temporaryPassword) {
    }
}

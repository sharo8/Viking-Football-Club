package rw.vikingfc.club.player;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;
import rw.vikingfc.club.user.UserService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerAccountService {

    private final PlayerRepository players;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public Map<String, Object> open(Long playerId, String email) {
        Player player = players.findById(playerId).orElseThrow(() -> NotFoundException.of("Player", playerId));
        String address = email == null || email.isBlank() ? player.getEmail() : email.trim().toLowerCase();
        if (address == null || address.isBlank()) {
            throw BusinessException.badRequest("A player account needs an email", "EMAIL_REQUIRED");
        }
        if (player.getUserId() != null) {
            throw BusinessException.conflict("This player already has a login", "ACCOUNT_EXISTS", null);
        }
        users.findByEmailIgnoreCaseAndDeletedAtIsNull(address).ifPresent(existing -> {
            throw BusinessException.conflict("A user with this email already exists", "EMAIL_TAKEN", null);
        });
        User user = new User();
        user.setEmail(address);
        user.setFullName(player.getFullName());
        user.setPhone(player.getPhone());
        user.setRole(Role.PLAYER);
        user.setRoleCode("PLAYER");
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setPasswordHash(passwordEncoder.encode(UserService.TEMP_PASSWORD));
        user.setFailedAttempts(0);
        users.save(user);
        player.setEmail(address);
        player.setUserId(user.getId());
        auditService.record("PLAYER_ACCOUNT", "Player", player.getId(), null, user.getEmail(), null);
        return Map.of("userId", user.getId(), "email", user.getEmail(), "temporaryPassword", UserService.TEMP_PASSWORD);
    }
}

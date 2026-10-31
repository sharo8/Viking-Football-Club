package rw.vikingfc.club.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.auth.RefreshTokenRepository;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.common.util.SecurityUtils;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    public static final String TEMP_PASSWORD = "ChangeMe@2026";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDtos.View> list() {
        return userRepository.findByDeletedAtIsNullOrderByFullNameAsc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public UserDtos.View get(Long id) {
        return toView(require(id));
    }

    public UserDtos.Credentials create(UserDtos.CreateRequest request) {
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email()).ifPresent(existing -> {
            throw BusinessException.conflict("A user with this email already exists");
        });
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setPhone(request.phone());
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setPasswordHash(passwordEncoder.encode(TEMP_PASSWORD));
        user.setFailedAttempts(0);
        userRepository.save(user);
        return new UserDtos.Credentials(user.getId(), user.getEmail(), TEMP_PASSWORD);
    }

    public UserDtos.View update(Long id, UserDtos.UpdateRequest request) {
        User user = require(id);
        guardLastExecutive(user, request.role(), request.active());
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setPhone(request.phone());
        user.setActive(request.active());
        if (!request.active()) {
            refreshTokenRepository.revokeAllForUser(user.getId());
        }
        return toView(user);
    }

    public UserDtos.View disable(Long id) {
        User user = require(id);
        if (user.getEmail().equalsIgnoreCase(SecurityUtils.email())) {
            throw BusinessException.conflict("You cannot disable your own account");
        }
        guardLastExecutive(user, user.getRole(), false);
        user.setActive(false);
        refreshTokenRepository.revokeAllForUser(user.getId());
        return toView(user);
    }

    public UserDtos.View enable(Long id) {
        User user = require(id);
        user.setActive(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        return toView(user);
    }

    public UserDtos.Credentials resetPassword(Long id) {
        User user = require(id);
        String temporary = "Viking@" + (100000 + new Random().nextInt(900000));
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        refreshTokenRepository.revokeAllForUser(user.getId());
        return new UserDtos.Credentials(user.getId(), user.getEmail(), temporary);
    }

    public User require(Long id) {
        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }

    public UserDtos.View toView(User user) {
        return new UserDtos.View(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getRole().isExecutive(),
                user.getPhone(),
                user.isActive(),
                user.isMustChangePassword()
        );
    }

    private void guardLastExecutive(User user, Role newRole, boolean active) {
        boolean staysExecutive = active && newRole.isExecutive();
        if (user.getRole().isExecutive() && user.isActive() && !staysExecutive) {
            long executives = userRepository.countByRoleInAndActiveTrueAndDeletedAtIsNull(
                    List.of(Role.PRESIDENT, Role.GENERAL_MANAGER, Role.FINANCE_MANAGER));
            if (executives <= 1) {
                throw BusinessException.conflict("The club must keep at least one active executive account");
            }
        }
    }
}

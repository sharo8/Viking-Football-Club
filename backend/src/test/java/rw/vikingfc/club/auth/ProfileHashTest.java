package rw.vikingfc.club.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import rw.vikingfc.club.audit.AuditService;
import rw.vikingfc.club.config.AppProperties;
import rw.vikingfc.club.security.JwtService;
import rw.vikingfc.club.security.LoginRateLimiter;
import rw.vikingfc.club.user.ClubRoleRepository;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;
import rw.vikingfc.club.user.UserService;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileHashTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void emailUpdateLeavesThePasswordHashUntouched() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        User user = new User();
        user.setId(2L);
        user.setEmail("generalmanager@vikingfc.rw");
        user.setFullName("Joseph Courbon");
        user.setRole(Role.GENERAL_MANAGER);
        user.setPasswordHash("stored-hash");
        user.setActive(true);
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("generalmanager@vikingfc.rw")).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("aline@example.com")).thenReturn(Optional.empty());
        when(jwt.accessToken(user)).thenReturn("token");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("generalmanager@vikingfc.rw", null, List.of()));
        AuthService service = new AuthService(
                users, mock(RefreshTokenRepository.class), mock(PasswordResetTokenRepository.class),
                encoder, jwt, mock(LoginRateLimiter.class), mock(AppProperties.class), mock(AuditService.class),
                Clock.systemUTC(), mock(OtpService.class), mock(ClubRoleRepository.class), mock(UserService.class),
                mock(LoginGuard.class));

        service.updateProfile("Aline@Example.com", null);

        assertEquals("stored-hash", user.getPasswordHash());
        assertEquals("aline@example.com", user.getEmail());
        verify(encoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }
}

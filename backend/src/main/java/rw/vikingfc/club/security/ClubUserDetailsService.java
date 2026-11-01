package rw.vikingfc.club.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rw.vikingfc.club.user.ClubRoleRepository;
import rw.vikingfc.club.user.User;
import rw.vikingfc.club.user.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ClubRoleRepository clubRoles;
    private final Clock clock;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now(clock));
        return new UserPrincipal(user, !locked, permissions(user));
    }

    private List<String> permissions(User user) {
        List<String> extra = new ArrayList<>();
        String code = user.getRoleCode() == null || user.getRoleCode().isBlank() ? user.getRole().name() : user.getRoleCode();
        clubRoles.findByCode(code).ifPresent(role -> {
            String stored = role.getPermissionsJson();
            if (stored != null && stored.contains("ROLES_MANAGE")) {
                extra.add("ROLES_MANAGE");
            }
        });
        return extra;
    }
}

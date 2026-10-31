package rw.vikingfc.club.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import rw.vikingfc.club.security.UserPrincipal;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean hasRole(String role) {
        Authentication authentication = authentication();
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public static boolean isExecutive() {
        return hasRole("EXECUTIVE");
    }

    public static String email() {
        Authentication authentication = authentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "system";
        }
        return authentication.getName();
    }

    public static UserPrincipal principal() {
        Authentication authentication = authentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }
}

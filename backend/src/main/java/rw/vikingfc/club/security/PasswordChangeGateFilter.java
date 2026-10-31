package rw.vikingfc.club.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import rw.vikingfc.club.common.api.ApiResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PasswordChangeGateFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                && principal.isMustChangePassword() && !allowed(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail(
                    "Change the temporary password before using the office.",
                    "PASSWORD_CHANGE_REQUIRED"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allowed(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        String method = request.getMethod();
        return ("POST".equals(method) && "/api/auth/change-password".equals(path))
                || ("POST".equals(method) && "/api/auth/logout".equals(path))
                || ("GET".equals(method) && "/api/auth/me".equals(path));
    }
}

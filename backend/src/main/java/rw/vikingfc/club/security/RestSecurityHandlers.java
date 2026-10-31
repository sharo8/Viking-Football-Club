package rw.vikingfc.club.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import rw.vikingfc.club.common.api.ApiResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestSecurityHandlers {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> write(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(response, HttpServletResponse.SC_FORBIDDEN,
                "You do not have access to this resource");
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(message));
    }
}

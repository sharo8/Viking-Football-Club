package rw.vikingfc.club.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionTrackingService sessions;

    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(HttpServletRequest request) {
        sessions.heartbeat(request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ApiResponse.ok("Session updated", null);
    }

    @PostMapping("/end")
    public ApiResponse<Void> end(@RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? "LOGOUT" : body.getOrDefault("reason", "LOGOUT");
        sessions.end(reason);
        return ApiResponse.ok("Session ended", null);
    }
}

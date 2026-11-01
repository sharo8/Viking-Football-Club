package rw.vikingfc.club.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.user.UserService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevPasswordController implements ApplicationRunner {

    private final AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Startup does not change any password. A temporary password is applied only when POST /api/dev/users/reset-password is called.");
    }

    @PostMapping("/users/reset-password")
    @SecurityRequirements
    public ApiResponse<Map<String, String>> reset(@RequestBody Map<String, String> body) {
        String email = body == null ? null : body.get("email");
        authService.devResetTemporaryPassword(email);
        return ApiResponse.ok("Temporary password restored", Map.of(
                "email", email == null ? "" : email.trim(),
                "temporaryPassword", UserService.TEMP_PASSWORD));
    }
}

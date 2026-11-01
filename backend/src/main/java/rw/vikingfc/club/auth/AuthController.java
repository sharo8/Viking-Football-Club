package rw.vikingfc.club.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.exception.BusinessException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login with email and password")
    public ApiResponse<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                      HttpServletRequest http) {
        return ApiResponse.ok("Logged in", authService.login(request, clientIp(http)));
    }

    @PostMapping("/otp/verify-login")
    @SecurityRequirements
    public ApiResponse<AuthDtos.LoginResponse> verifyLogin(@RequestBody java.util.Map<String, String> body) {
        return ApiResponse.ok("Logged in", authService.verifyLoginOtp(body.get("challengeId"), body.get("code")));
    }

    @PostMapping("/otp/resend-login")
    @SecurityRequirements
    public ApiResponse<Void> resendLogin(@RequestBody java.util.Map<String, String> body, HttpServletRequest http) {
        authService.resendLoginOtp(body.get("challengeId"), clientIp(http));
        return ApiResponse.ok("A new code is on its way.", null);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    public ApiResponse<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Revoke a refresh token")
    public ApiResponse<Void> logout(@RequestBody(required = false) AuthDtos.RefreshRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return ApiResponse.ok("Logged out", null);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the password of the signed-in user")
    public ApiResponse<AuthDtos.TokenResponse> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        return ApiResponse.ok("Password changed", authService.changePassword(request));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Create a password reset token")
    public ApiResponse<Void> forgot(@Valid @RequestBody AuthDtos.ForgotRequest request, HttpServletRequest http) {
        try {
            otpService.request(request.email(), OtpService.PASSWORD_RESET, clientIp(http));
        } catch (BusinessException ex) {
            if (!"OTP_COOLDOWN".equals(ex.getCode()) && !"OTP_RATE".equals(ex.getCode())) {
                throw ex;
            }
        }
        return ApiResponse.ok("If that email belongs to a club account, a code is on its way.", null);
    }

    @PostMapping("/otp/request")
    @SecurityRequirements
    public ApiResponse<Void> otpRequest(@RequestBody java.util.Map<String, String> body, HttpServletRequest http) {
        otpService.request(body.get("email"), body.getOrDefault("purpose", OtpService.PASSWORD_RESET), clientIp(http));
        return ApiResponse.ok("If that email belongs to a club account, a code is on its way.", null);
    }

    @PostMapping("/otp/verify")
    @SecurityRequirements
    public ApiResponse<Void> otpVerify(@RequestBody java.util.Map<String, String> body) {
        otpService.verify(body.get("email"), body.getOrDefault("purpose", OtpService.PASSWORD_RESET), body.get("code"));
        return ApiResponse.ok("Code accepted", null);
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Set a new password with a reset token")
    public ApiResponse<Void> reset(@RequestBody java.util.Map<String, String> body) {
        if (body.get("otp") != null || body.get("code") != null) {
            otpService.resetPassword(body.get("email"), body.getOrDefault("otp", body.get("code")), body.get("newPassword"));
        } else {
            authService.resetWithToken(new AuthDtos.ResetWithTokenRequest(body.get("token"), body.get("newPassword")));
        }
        return ApiResponse.ok("Password updated", null);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user")
    public ApiResponse<AuthDtos.Me> me() {
        return ApiResponse.ok("Current user", authService.me());
    }

    @PatchMapping("/me")
    @Operation(summary = "Change the signed-in user's email and phone")
    public ApiResponse<java.util.Map<String, Object>> updateProfile(@RequestBody java.util.Map<String, String> body) {
        return ApiResponse.ok("Profile updated", authService.updateProfile(body.get("email"), body.containsKey("phone") ? body.get("phone") : null));
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Store the signed-in user's photo")
    public ApiResponse<java.util.Map<String, Object>> photo(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Photo saved", authService.updatePhoto(file));
    }

    @DeleteMapping("/me/photo")
    @Operation(summary = "Remove the signed-in user's photo")
    public ApiResponse<java.util.Map<String, Object>> clearPhoto() {
        return ApiResponse.ok("Photo removed", authService.clearPhoto());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package rw.vikingfc.club.removal;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RemovalController {

    private final RoleRemovalService roles;
    private final AccountRemovalService accounts;
    private final FormerPlayerService former;

    @GetMapping("/api/roles/{id}/usage")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> usage(@PathVariable Long id) {
        return ApiResponse.ok("Usage", roles.usage(id));
    }

    @PostMapping("/api/roles/{id}/resolve-and-delete")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> resolve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Role deleted", roles.resolveAndDelete(id, body));
    }

    @PostMapping("/api/users/{id}/remove")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> removeUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Account removed", accounts.removeUser(id, body.get("reasonCode"), body.get("reason"), body.get("confirmName")));
    }

    @GetMapping("/api/users/removed")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<List<Map<String, Object>>> removed() {
        return ApiResponse.ok("Removed accounts", accounts.removed());
    }

    @PostMapping("/api/users/{id}/restore-removed")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> restoreUser(@PathVariable Long id) {
        return ApiResponse.ok("Account restored", accounts.restore(id));
    }

    @PostMapping("/api/staff/{id}/remove")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> removeStaff(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Staff removed", accounts.removeStaff(id, body.get("reasonCode"), body.get("reason"), body.get("confirmName")));
    }

    @PostMapping("/api/players/{id}/former")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> formerPlayer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Moved to former players", former.move(id, body));
    }

    @GetMapping("/api/former-players")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> formerPlayers(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) String exitReason,
                                                          @RequestParam(required = false) String year,
                                                          @RequestParam(required = false) String history,
                                                          @RequestParam(required = false) String deletion) {
        return ApiResponse.ok("Former players", former.list(q, exitReason, year, history, deletion));
    }

    @GetMapping("/api/former-players/kpis")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> kpis() {
        return ApiResponse.ok("Former players", former.kpis());
    }

    @PostMapping("/api/former-players/{id}/restore")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> restoreFormer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Player restored", former.restore(id, Boolean.TRUE.equals(body.get("reuseContract"))));
    }

    @PostMapping("/api/former-players/{id}/deletion-requests")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> ask(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Request sent", former.request(id, body.get("reason"), body.get("confirmName")));
    }

    @GetMapping("/api/deletion-requests")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<List<Map<String, Object>>> queue(@RequestParam(required = false) String status) {
        return ApiResponse.ok("Requests", former.queue(status));
    }

    @GetMapping("/api/deletion-requests/{id}")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> request(@PathVariable Long id) {
        return ApiResponse.ok("Request", former.one(id));
    }

    @PostMapping("/api/deletion-requests/{id}/approve")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Vote saved", former.approve(id, body.get("comment"), body.get("otp")));
    }

    @PostMapping("/api/deletion-requests/{id}/reject")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Vote saved", former.reject(id, body.get("comment"), body.get("otp")));
    }
}

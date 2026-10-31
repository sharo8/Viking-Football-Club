package rw.vikingfc.club.dashboard;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.api.PageResponse;
import rw.vikingfc.club.medical.MedicalService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboards")
public class DashboardController {

    private final DashboardService dashboardService;
    private final MedicalService medicalService;
    private final ClubHomeService clubHomeService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> summary(@RequestParam(defaultValue = "season") String period) {
        return ApiResponse.ok("Dashboard summary", clubHomeService.summary(period));
    }

    @GetMapping("/next-match")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> nextMatch() {
        return ApiResponse.ok("Next match", clubHomeService.nextMatch());
    }

    @GetMapping("/recent-matches")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','ASSISTANT_MANAGER','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<PageResponse<Map<String, Object>>> recentMatches(
            @RequestParam(defaultValue = "season") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok("Recent matches", clubHomeService.recentMatches(period, page, size));
    }

    @GetMapping("/top-scorers")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','ASSISTANT_MANAGER','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<PageResponse<Map<String, Object>>> topScorers(
            @RequestParam(defaultValue = "season") String scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok("Top scorers", clubHomeService.topScorers(scope, page, size));
    }

    @GetMapping("/top-assists")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','ASSISTANT_MANAGER','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<PageResponse<Map<String, Object>>> topAssists(
            @RequestParam(defaultValue = "season") String scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok("Top assists", clubHomeService.topAssists(scope, page, size));
    }

    @GetMapping("/recent-goals")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','ASSISTANT_MANAGER','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<PageResponse<Map<String, Object>>> recentGoals(
            @RequestParam(defaultValue = "season") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok("Recent goals", clubHomeService.recentGoals(period, page, size));
    }

    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> alerts() {
        return ApiResponse.ok("Alerts", clubHomeService.alerts());
    }

    @GetMapping("/activity")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<Map<String, Object>>> activity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok("Recent activity", clubHomeService.activity(page, size));
    }

    @GetMapping("/executive")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> executive() {
        return ApiResponse.ok("Executive dashboard", dashboardService.executive());
    }

    @GetMapping("/coach")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<Map<String, Object>> coach() {
        return ApiResponse.ok("Coach dashboard", dashboardService.coach());
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasAnyRole('EXECUTIVE','DOCTOR')")
    public ApiResponse<Map<String, Object>> doctor() {
        return ApiResponse.ok("Doctor dashboard", medicalService.dashboard());
    }

    @GetMapping("/assistant")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> assistant() {
        return ApiResponse.ok("Operations dashboard", dashboardService.assistant());
    }

    @GetMapping("/media")
    @PreAuthorize("hasAnyRole('EXECUTIVE','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> media() {
        return ApiResponse.ok("Media dashboard", dashboardService.media());
    }

    @GetMapping("/charts/{name}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Object> charts(@PathVariable String name, @RequestParam(defaultValue = "season") String period) {
        return ApiResponse.ok("Chart series", clubHomeService.charts(name, period));
    }
}

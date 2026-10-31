package rw.vikingfc.club.marketing;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.user.Role;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Marketing")
public class MarketingController {

    private final MarketingService marketingService;
    private final NotificationService notificationService;

    @GetMapping("/api/marketing/calendar")
    @PreAuthorize("hasAnyRole('EXECUTIVE','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> calendar() {
        return ApiResponse.ok("Content calendar", marketingService.calendar());
    }

    @PostMapping("/api/marketing/posts")
    @PreAuthorize("hasAnyRole('EXECUTIVE','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> post(@Valid @RequestBody MarketingService.PostWrite request) {
        return ApiResponse.ok("Post saved", marketingService.createPost(request));
    }

    @PostMapping("/api/marketing/posts/{id}/publish")
    @PreAuthorize("hasAnyRole('EXECUTIVE','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> publish(@PathVariable Long id) {
        return ApiResponse.ok("Post published", marketingService.publishPost(id));
    }

    @GetMapping("/api/marketing/campaigns")
    @PreAuthorize("hasAnyRole('EXECUTIVE','MARKETING_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> campaigns() {
        return ApiResponse.ok("Campaigns", marketingService.campaigns());
    }

    @PostMapping("/api/marketing/campaigns")
    @PreAuthorize("hasAnyRole('EXECUTIVE','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> campaign(@Valid @RequestBody MarketingService.CampaignWrite request) {
        return ApiResponse.ok("Campaign saved", marketingService.createCampaign(request));
    }

    @PutMapping("/api/marketing/campaigns/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> updateCampaign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CampaignStatus status = body.get("status") == null ? null : CampaignStatus.valueOf(body.get("status").toString());
        Long reach = body.get("reach") == null ? null : ((Number) body.get("reach")).longValue();
        Long engagement = body.get("engagement") == null ? null : ((Number) body.get("engagement")).longValue();
        return ApiResponse.ok("Campaign updated", marketingService.updateCampaign(id, status,
                body.get("results") == null ? null : body.get("results").toString(), reach, engagement));
    }

    @GetMapping("/api/marketing/matchday-tasks")
    @PreAuthorize("hasAnyRole('EXECUTIVE','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> tasks(@RequestParam(required = false) Long matchId) {
        return ApiResponse.ok("Match-day checklist", marketingService.tasks(matchId));
    }

    @PostMapping("/api/marketing/matchday-tasks")
    @PreAuthorize("hasAnyRole('EXECUTIVE','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> addTask(@RequestBody Map<String, String> body) {
        return ApiResponse.ok("Task added", marketingService.addTask(Long.valueOf(body.get("matchId")), body.get("label"), body.get("ownerRole")));
    }

    @PostMapping("/api/marketing/matchday-tasks/{id}/toggle")
    @PreAuthorize("hasAnyRole('EXECUTIVE','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> toggle(@PathVariable Long id) {
        return ApiResponse.ok("Task updated", marketingService.toggleTask(id));
    }

    @GetMapping("/api/announcements")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> announcements() {
        return ApiResponse.ok("Announcements", marketingService.announcements());
    }

    @PostMapping("/api/announcements")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> announce(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) body.get("roles");
        Set<Role> audience = roles == null ? Set.of() : roles.stream().map(Role::valueOf).collect(Collectors.toSet());
        return ApiResponse.ok("Announcement sent", marketingService.announce(body.get("title").toString(), body.get("body").toString(), audience));
    }

    @GetMapping("/api/notifications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> notifications() {
        return ApiResponse.ok("Notifications", notificationService.mineView());
    }

    @PostMapping("/api/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> read(@PathVariable Long id) {
        return ApiResponse.ok("Notification read", notificationService.markRead(id).getId());
    }
}

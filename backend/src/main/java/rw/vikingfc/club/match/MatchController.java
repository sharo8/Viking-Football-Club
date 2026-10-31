package rw.vikingfc.club.match;

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
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Matches")
public class MatchController {

    private final MatchService matchService;
    private final AnalyticsService analyticsService;

    @GetMapping("/api/matches")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Matches", matchService.list());
    }

    @GetMapping("/api/matches/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.ok("Match", matchService.get(id));
    }

    @PostMapping("/api/matches")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody MatchService.MatchWrite request) {
        return ApiResponse.ok("Match created", matchService.create(request));
    }

    @PutMapping("/api/matches/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody MatchService.MatchWrite request) {
        return ApiResponse.ok("Match updated", matchService.update(id, request));
    }

    @PostMapping("/api/matches/{id}/events")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> event(@PathVariable Long id, @Valid @RequestBody MatchService.EventWrite request) {
        return ApiResponse.ok("Event recorded", matchService.addEvent(id, request));
    }

    @GetMapping("/api/matches/{id}/events")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> events(@PathVariable Long id) {
        return ApiResponse.ok("Events", matchService.events(id));
    }

    @GetMapping("/api/matches/{id}/records")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> records(@PathVariable Long id) {
        return ApiResponse.ok("Match records", matchService.records(id));
    }

    @PostMapping("/api/matches/{id}/players/{playerId}/late")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> late(@PathVariable Long id, @PathVariable Long playerId,
                                                  @RequestBody Map<String, Object> body) {
        int minutes = ((Number) body.getOrDefault("minutesLate", 0)).intValue();
        boolean justified = Boolean.TRUE.equals(body.get("justified"));
        return ApiResponse.ok("Late arrival recorded", matchService.markLate(id, playerId, minutes, justified));
    }

    @PutMapping("/api/matches/{id}/players/{playerId}/rating")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> rating(@PathVariable Long id, @PathVariable Long playerId,
                                                    @RequestBody Map<String, BigDecimal> body) {
        return ApiResponse.ok("Rating saved", matchService.rate(id, playerId, body.get("rating")));
    }

    @GetMapping("/api/opponents")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> opponents() {
        return ApiResponse.ok("Opponents", matchService.opponents());
    }

    @PostMapping("/api/opponents")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> opponent(@Valid @RequestBody MatchService.OpponentWrite request) {
        return ApiResponse.ok("Opponent saved", matchService.saveOpponent(request));
    }

    @GetMapping("/api/standings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> standings() {
        return ApiResponse.ok("Standings", matchService.standings());
    }

    @PutMapping("/api/standings")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> replaceStandings(@RequestBody List<MatchService.StandingWrite> rows) {
        return ApiResponse.ok("Standings saved", matchService.replaceStandings(rows));
    }

    @GetMapping("/api/analytics/leaderboards")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> leaderboards() {
        return ApiResponse.ok("Leaderboards", analyticsService.leaderboards());
    }
}

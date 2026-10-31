package rw.vikingfc.club.lineup;

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

import java.util.Map;

@RestController
@RequestMapping("/api/matches/{matchId}/lineup")
@RequiredArgsConstructor
@Tag(name = "Lineups")
public class LineupController {

    private final LineupService lineupService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> current(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup", lineupService.current(matchId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> save(@PathVariable Long matchId, @Valid @RequestBody LineupService.LineupWrite request) {
        return ApiResponse.ok("Lineup saved", lineupService.save(matchId, request));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> publish(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup published", lineupService.publish(matchId));
    }

    @PostMapping("/unpublish")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> unpublish(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup unpublished", lineupService.unpublish(matchId));
    }
}

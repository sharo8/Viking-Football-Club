package rw.vikingfc.club.competition;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.match.MatchAlertService;
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.match.StandingsService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;
    private final CalendarService calendarService;
    private final StandingsService standingsService;
    private final MatchAlertService alertService;

    @GetMapping("/api/competitions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Competitions", competitionService.list());
    }

    @PostMapping("/api/competitions")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Competition created", competitionService.create(
                String.valueOf(body.get("name")),
                body.get("type") == null ? CompetitionType.OTHER : CompetitionType.valueOf(String.valueOf(body.get("type"))),
                body.get("official") == null ? null : Boolean.valueOf(String.valueOf(body.get("official"))),
                body.get("countsForTable") == null ? null : Boolean.valueOf(String.valueOf(body.get("countsForTable"))),
                body.get("color") == null ? null : String.valueOf(body.get("color")),
                body.get("squadSize") == null ? null : ((Number) body.get("squadSize")).intValue(),
                body.get("sourceLang") == null ? null : String.valueOf(body.get("sourceLang")),
                body.get("nameFr") == null ? null : String.valueOf(body.get("nameFr")),
                body.get("nameRw") == null ? null : String.valueOf(body.get("nameRw"))));
    }

    @PutMapping("/api/competitions/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Competition updated", competitionService.update(id,
                body.get("name") == null ? null : String.valueOf(body.get("name")),
                body.get("type") == null ? null : CompetitionType.valueOf(String.valueOf(body.get("type"))),
                body.get("official") == null ? null : Boolean.valueOf(String.valueOf(body.get("official"))),
                body.get("countsForTable") == null ? null : Boolean.valueOf(String.valueOf(body.get("countsForTable"))),
                body.get("color") == null ? null : String.valueOf(body.get("color")),
                body.get("archived") == null ? null : Boolean.valueOf(String.valueOf(body.get("archived")))));
    }

    @PostMapping("/api/competitions/{id}/teams")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> addTeam(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long opponentId = body.get("opponentId") == null ? null : ((Number) body.get("opponentId")).longValue();
        return ApiResponse.ok("Team added", competitionService.addTeam(id, opponentId,
                body.get("name") == null ? null : String.valueOf(body.get("name")),
                body.get("shortName") == null ? null : String.valueOf(body.get("shortName")),
                body.get("city") == null ? null : String.valueOf(body.get("city"))));
    }

    @GetMapping("/api/calendar")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> calendar(@RequestParam String from, @RequestParam String to,
                                                           @RequestParam(required = false) String layers) {
        return ApiResponse.ok("Calendar", calendarService.events(Instant.parse(from), Instant.parse(to), layers));
    }

    @GetMapping(value = "/api/calendar/ics", produces = "text/calendar")
    @PreAuthorize("isAuthenticated()")
    public String ics(@RequestParam String from, @RequestParam String to) {
        return calendarService.ics(Instant.parse(from), Instant.parse(to));
    }

    @PostMapping("/api/league-fixtures")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> fixture(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Fixture saved", calendarService.fixture(
                ((Number) body.get("competitionId")).longValue(),
                ((Number) body.get("homeId")).longValue(),
                ((Number) body.get("awayId")).longValue(),
                Instant.parse(String.valueOf(body.get("kickoff"))),
                body.get("matchday") == null ? null : ((Number) body.get("matchday")).intValue(),
                body.get("goalsHome") == null ? null : ((Number) body.get("goalsHome")).intValue(),
                body.get("goalsAway") == null ? null : ((Number) body.get("goalsAway")).intValue(),
                body.get("status") == null ? null : MatchStatus.valueOf(String.valueOf(body.get("status")))));
    }

    @GetMapping("/api/standings/{competitionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> table(@PathVariable Long competitionId) {
        return ApiResponse.ok("Standings", standingsService.table(competitionId));
    }

    @PostMapping("/api/standings/{competitionId}/recalculate")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> recalculate(@PathVariable Long competitionId) {
        return ApiResponse.ok("Standings updated", standingsService.recalculate(competitionId));
    }

    @PostMapping("/api/standings/{competitionId}/correct")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> correct(@PathVariable Long competitionId, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Correction saved", standingsService.correct(competitionId,
                String.valueOf(body.get("teamName")),
                ((Number) body.get("pointsDelta")).intValue(),
                String.valueOf(body.get("reason"))));
    }

    @GetMapping("/api/alerts/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> alerts() {
        return ApiResponse.ok("Alerts", alertService.mine());
    }

    @PostMapping("/api/alerts/run")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Integer> run(@RequestParam(required = false) String asOf) {
        return ApiResponse.ok("Alerts checked", alertService.run(asOf == null || asOf.isBlank() ? null : Instant.parse(asOf)));
    }
}

package rw.vikingfc.club.match;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.report.DirectorySheets;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Matches")
public class MatchController {

    private final MatchService matchService;
    private final AnalyticsService analyticsService;
    private final DirectorySheets sheets;

    @GetMapping("/api/matches")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) Long competitionId,
                                                        @RequestParam(required = false) String type,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Boolean home,
                                                        @RequestParam(required = false) String q,
                                                        @RequestParam(required = false) Boolean needsAttention) {
        return ApiResponse.ok("Matches", matchService.search(competitionId, type, status, home, q, needsAttention));
    }

    @GetMapping("/api/matches/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> export(@RequestParam String format, @RequestParam(defaultValue = "en") String lang, @RequestParam(required = false) String q) {
        List<String> headers = "fr".equals(lang)
                ? List.of("Coup d'envoi", "Adversaire", "Lieu", "Domicile", "Statut")
                : "rw".equals(lang)
                ? List.of("Itangira", "Abasangana", "Ahantu", "Iwacu", "Imiterere")
                : List.of("Kick-off", "Opponent", "Venue", "Home", "Status");
        String title = "fr".equals(lang) ? "Matchs" : "rw".equals(lang) ? "Imikino" : "Matches";
        List<List<String>> rows = matchService.search(null, null, null, null, q, null).stream().map(row -> List.of(
                String.valueOf(row.getOrDefault("kickoff", "")),
                String.valueOf(row.getOrDefault("opponent", "")),
                String.valueOf(row.getOrDefault("venue", "")),
                String.valueOf(row.getOrDefault("home", "")),
                String.valueOf(row.getOrDefault("status", ""))
        )).toList();
        boolean pdf = "pdf".equalsIgnoreCase(format);
        var principal = SecurityUtils.principal();
        String who = principal == null ? SecurityUtils.email() : principal.getFullName();
        byte[] body = pdf ? sheets.pdf(title, who, headers, rows, lang) : sheets.excel(title, who, headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=matches." + (pdf ? "pdf" : "xlsx"))
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
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

    @DeleteMapping("/api/matches/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Long> delete(@PathVariable Long id) {
        matchService.delete(id);
        return ApiResponse.ok("Match deleted", id);
    }

    @PostMapping("/api/matches/{id}/result")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> result(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int goalsFor = ((Number) body.getOrDefault("goalsFor", 0)).intValue();
        int goalsAgainst = ((Number) body.getOrDefault("goalsAgainst", 0)).intValue();
        Integer crowd = body.get("crowd") == null ? null : ((Number) body.get("crowd")).intValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scorers = (List<Map<String, Object>>) body.getOrDefault("scorers", List.of());
        List<MatchService.Scorer> rows = scorers.stream().map(row -> new MatchService.Scorer(
                ((Number) row.getOrDefault("minute", 1)).intValue(),
                row.get("playerId") == null ? null : ((Number) row.get("playerId")).longValue(),
                row.get("assistId") == null ? null : ((Number) row.get("assistId")).longValue()
        )).toList();
        return ApiResponse.ok("Result saved", matchService.recordResult(id, goalsFor, goalsAgainst, crowd, rows));
    }

    @PutMapping("/api/matches/{id}/reschedule")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Map<String, Object>> reschedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Match rescheduled", matchService.reschedule(id, java.time.Instant.parse(body.get("kickoff"))));
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

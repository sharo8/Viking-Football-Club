package rw.vikingfc.club.lineup;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LineupCenterController {

    private final LineupCenterService center;

    @GetMapping("/api/lineups/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> sheet(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup", center.sheet(matchId));
    }

    @PutMapping("/api/lineups/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> save(@PathVariable Long matchId, @RequestBody LineupService.LineupWrite request) {
        return ApiResponse.ok("Lineup saved", center.save(matchId, request));
    }

    @PostMapping("/api/lineups/{matchId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> submit(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup submitted", center.transition(matchId, "submit", null));
    }

    @PostMapping("/api/lineups/{matchId}/approve")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup approved", center.transition(matchId, "approve", null));
    }

    @PostMapping("/api/lineups/{matchId}/request-changes")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> changes(@PathVariable Long matchId, @RequestBody Note note) {
        return ApiResponse.ok("Changes requested", center.transition(matchId, "changes", note.comment()));
    }

    @PostMapping("/api/lineups/{matchId}/reject")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long matchId, @RequestBody Note note) {
        return ApiResponse.ok("Lineup rejected", center.transition(matchId, "reject", note.comment()));
    }

    @PostMapping("/api/lineups/{matchId}/publish")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> publish(@PathVariable Long matchId, @RequestBody(required = false) Note note) {
        return ApiResponse.ok("Lineup published", center.transition(matchId, "publish", note == null ? null : note.comment()));
    }

    @PostMapping("/api/lineups/{matchId}/unpublish")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> unpublish(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup unpublished", center.transition(matchId, "unpublish", null));
    }

    @PostMapping("/api/lineups/{matchId}/unlock")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> unlock(@PathVariable Long matchId, @RequestBody Note note) {
        return ApiResponse.ok("Lineup unlocked", center.transition(matchId, "unlock", note.comment()));
    }

    @PostMapping("/api/lineups/{matchId}/suggest-gk")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> suggest(@PathVariable Long matchId, @RequestBody Suggest body) {
        return ApiResponse.ok("Goalkeeper suggested", center.suggestGoalkeeper(matchId, body.playerId()));
    }

    @GetMapping("/api/lineups/{matchId}/versions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> versions(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup history", center.sheet(matchId));
    }

    @PostMapping("/api/lineups/{matchId}/restore/{revision}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> restore(@PathVariable Long matchId, @PathVariable int revision) {
        return ApiResponse.ok("Version restored", center.restore(matchId, revision));
    }

    @PutMapping("/api/convocations/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> calls(@PathVariable Long matchId, @RequestBody LineupCenterService.CallWrite request) {
        return ApiResponse.ok("Call-ups saved", center.saveCalls(matchId, request));
    }

    @GetMapping("/api/convocations/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> callSheet(@PathVariable Long matchId) {
        return sheet(matchId);
    }

    @PostMapping("/api/convocations/{matchId}/send")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> send(@PathVariable Long matchId) {
        return ApiResponse.ok("Convocations sent", center.sendCalls(matchId, false));
    }

    @PostMapping("/api/convocations/{matchId}/remind")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> remind(@PathVariable Long matchId) {
        return ApiResponse.ok("Reminders sent", center.sendCalls(matchId, true));
    }

    @PostMapping("/api/convocations/{matchId}/not-selected")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> omitted(@PathVariable Long matchId) {
        return ApiResponse.ok("Updates sent", center.notifyOmitted(matchId));
    }

    @PostMapping("/api/lineups/{matchId}/substitutions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> sub(@PathVariable Long matchId, @RequestBody Sub body) {
        return ApiResponse.ok("Substitution recorded", center.substitute(matchId, body.outId(), body.inId(), body.minute(), body.reason()));
    }

    @GetMapping("/api/matches/{matchId}/ratings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> ratings(@PathVariable Long matchId) {
        return sheet(matchId);
    }

    @PutMapping("/api/matches/{matchId}/ratings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> rate(@PathVariable Long matchId, @RequestBody Rate body) {
        return ApiResponse.ok("Rating saved", center.rate(matchId, body.playerId(), body.rating(), body.comment(), body.motm()));
    }

    @PostMapping("/api/matches/{matchId}/ratings/close")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> close(@PathVariable Long matchId) {
        return ApiResponse.ok("Ratings closed", center.closeRatings(matchId));
    }

    @GetMapping("/api/ratings/leaderboard")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> leaderboard() {
        return ApiResponse.ok("Ratings", center.leaderboard());
    }

    @PostMapping("/api/lineups/{matchId}/flags")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> flag(@PathVariable Long matchId, @RequestBody Flag body) {
        return ApiResponse.ok("Flag saved", center.flag(matchId, body.playerId(), body.note()));
    }

    @GetMapping("/api/formations")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<String>> formations() {
        return ApiResponse.ok("Formations", FormationBook.codes());
    }

    @GetMapping(value = "/api/lineups/{matchId}/graphic", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> graphic(@PathVariable Long matchId) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Viking_FC_Matchday.png\"").body(art(matchId, true));
    }

    @GetMapping(value = "/api/lineups/{matchId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> pdf(@PathVariable Long matchId) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Viking_FC_Matchsheet.pdf\"").body(art(matchId, false));
    }

    private byte[] art(Long matchId, boolean png) {
        Map<String, Object> sheet = center.sheet(matchId);
        @SuppressWarnings("unchecked")
        Map<String, Object> match = (Map<String, Object>) sheet.get("match");
        @SuppressWarnings("unchecked")
        Map<String, Object> lineup = (Map<String, Object>) sheet.get("lineup");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots = (List<Map<String, Object>>) sheet.getOrDefault("slots", List.of());
        List<LineupSheetArt.Token> tokens = new ArrayList<>();
        for (Map<String, Object> slot : slots) {
            if (!"STARTING".equals(String.valueOf(slot.get("selection")))) continue;
            tokens.add(new LineupSheetArt.Token(String.valueOf(slot.get("name")),
                    slot.get("jersey") == null ? "" : String.valueOf(slot.get("jersey")),
                    slot.get("x") instanceof Number number ? number.intValue() : 50,
                    slot.get("y") instanceof Number number ? number.intValue() : 50));
        }
        String opponent = match == null ? "Opponent" : String.valueOf(match.get("opponent"));
        String when = match == null ? "" : String.valueOf(match.get("kickoff"));
        String venue = match == null || match.get("venue") == null ? "" : String.valueOf(match.get("venue"));
        String formation = lineup == null || lineup.get("formation") == null ? "4-3-3" : String.valueOf(lineup.get("formation"));
        if (!png) {
            return LineupSheetArt.pdf(opponent, when, venue, formation, tokens);
        }
        byte[] logo = null;
        try (var stream = LineupCenterController.class.getResourceAsStream("/brand/logo.png")) {
            if (stream != null) logo = stream.readAllBytes();
        } catch (Exception ignored) {
            logo = null;
        }
        return LineupSheetArt.png("vs " + opponent, formation + " · " + venue, tokens, logo);
    }

    public record Note(String comment) {
    }

    public record Suggest(Long playerId) {
    }

    public record Sub(Long outId, Long inId, int minute, String reason) {
    }

    public record Rate(Long playerId, BigDecimal rating, String comment, Boolean motm) {
    }

    public record Flag(Long playerId, String note) {
    }
}

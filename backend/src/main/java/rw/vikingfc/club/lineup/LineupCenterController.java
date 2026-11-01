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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.KigaliClock;
import rw.vikingfc.club.common.api.ApiResponse;

import rw.vikingfc.club.config.AppProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LineupCenterController {

    private final LineupCenterService center;
    private final SquadBuilderService squads;
    private final AppProperties properties;

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

    @GetMapping("/api/lineups/{matchId}/history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> history(@PathVariable Long matchId,
                                                    @RequestParam(defaultValue = "all") String type,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "8") int size,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String user) {
        return ApiResponse.ok("Lineup history", center.historyPage(matchId, type, page, size, status, user));
    }

    @GetMapping("/api/lineups/{matchId}/versions/{left}/diff/{right}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> diff(@PathVariable Long matchId, @PathVariable int left, @PathVariable int right) {
        return ApiResponse.ok("Lineup diff", center.versionDiff(matchId, left, right));
    }

    @GetMapping("/api/lineups/{matchId}/graphic-data")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> graphicData(@PathVariable Long matchId) {
        return ApiResponse.ok("Matchday graphic", center.sheet(matchId));
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

    @PostMapping("/api/convocations/{matchId}/confirm-and-send")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> confirmAndSend(@PathVariable Long matchId) {
        return ApiResponse.ok("Call-ups sent", center.confirmAndSend(matchId));
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

    @PutMapping("/api/lineups/{matchId}/squad-size")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> squadSize(@PathVariable Long matchId, @RequestBody SquadBuilderService.SizeWrite request) {
        squads.saveSize(matchId, request);
        return ApiResponse.ok("Squad size saved", center.sheet(matchId));
    }

    @PutMapping("/api/lineups/{matchId}/squad")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> squad(@PathVariable Long matchId, @RequestBody SquadBuilderService.SquadWrite request) {
        squads.saveSquad(matchId, request);
        return ApiResponse.ok("Squad saved", center.sheet(matchId));
    }

    @PutMapping("/api/lineups/{matchId}/xi")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> xi(@PathVariable Long matchId, @RequestBody SquadBuilderService.XiWrite request) {
        squads.saveXi(matchId, request);
        return ApiResponse.ok("Starting XI saved", center.sheet(matchId));
    }

    @PostMapping("/api/lineups/{matchId}/suggest-squad")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> suggestSquad(@PathVariable Long matchId) {
        squads.suggest(matchId);
        return ApiResponse.ok("Squad suggested", center.sheet(matchId));
    }

    @PostMapping("/api/lineups/{matchId}/auto-xi")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> autoXi(@PathVariable Long matchId) {
        squads.autoXi(matchId);
        return ApiResponse.ok("Starting XI filled", center.sheet(matchId));
    }

    @PostMapping("/api/lineups/{matchId}/edit")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> edit(@PathVariable Long matchId) {
        return ApiResponse.ok("Lineup opened for editing", squads.edit(matchId));
    }

    @PostMapping("/api/lineups/{matchId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long matchId, @RequestBody(required = false) SquadBuilderService.ConfirmWrite request) {
        return ApiResponse.ok("Lineup confirmed", squads.confirm(matchId, request == null ? new SquadBuilderService.ConfirmWrite(null, null, null, null, null) : request));
    }

    @PostMapping("/api/lineups/{matchId}/copy-previous")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> copyPrevious(@PathVariable Long matchId) {
        squads.copyPrevious(matchId);
        return ApiResponse.ok("Previous lineup copied", center.sheet(matchId));
    }

    @PostMapping("/api/lineups/{matchId}/duplicate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> duplicate(@PathVariable Long matchId, @RequestBody SquadBuilderService.DuplicateWrite request) {
        squads.duplicate(matchId, request.targetMatchId());
        return ApiResponse.ok("Lineup duplicated", center.sheet(request.targetMatchId()));
    }

    @GetMapping("/api/matches/lineup-overview")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "8") int size,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String competition,
                                                     @RequestParam(required = false) Boolean home,
                                                     @RequestParam(required = false) String q) {
        return ApiResponse.ok("Matches", squads.overview(page, size, status, competition, home, q));
    }

    @GetMapping("/api/matches/{matchId}/participation")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> participation(@PathVariable Long matchId) {
        return ApiResponse.ok("Participation", squads.participation(matchId));
    }

    @GetMapping("/api/formations")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> formations() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String code : FormationBook.codes()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", code);
            row.put("rows", FormationBook.rowSizes(code));
            row.put("spots", FormationBook.spots(code));
            rows.add(row);
        }
        return ApiResponse.ok("Formations", rows);
    }

    @GetMapping(value = "/api/lineups/{matchId}/graphic", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> graphic(@PathVariable Long matchId,
                                         @RequestParam(defaultValue = "photo") String template,
                                         @RequestParam(defaultValue = "portrait") String ratio) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Viking_FC_Matchday.png\"").body(art(matchId, true, "en", template, ratio));
    }

    @GetMapping(value = "/api/lineups/{matchId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> pdf(@PathVariable Long matchId, @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Viking_FC_Matchsheet.pdf\"").body(art(matchId, false, lang, "photo", "portrait"));
    }

    private byte[] art(Long matchId, boolean png, String lang, String template, String ratio) {
        Map<String, Object> sheet = center.sheet(matchId);
        @SuppressWarnings("unchecked")
        Map<String, Object> match = (Map<String, Object>) sheet.get("match");
        @SuppressWarnings("unchecked")
        Map<String, Object> lineup = (Map<String, Object>) sheet.get("lineup");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots = (List<Map<String, Object>>) sheet.getOrDefault("slots", List.of());
        String formation = lineup == null || lineup.get("formation") == null ? "4-3-3" : String.valueOf(lineup.get("formation"));
        List<LineupSheetArt.Token> tokens = new ArrayList<>();
        List<LineupSheetArt.Token> bench = new ArrayList<>();
        for (Map<String, Object> slot : slots) {
            String code = slot.get("positionSlot") == null ? "" : String.valueOf(slot.get("positionSlot"));
            FormationBook.Spot spot = FormationBook.spots(formation).stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
            int x = spot == null ? 50 : spot.x();
            int y = spot == null ? 50 : spot.y();
            LineupSheetArt.Token token = new LineupSheetArt.Token(
                    slot.get("name") == null ? "" : String.valueOf(slot.get("name")),
                    slot.get("jersey") == null ? "" : String.valueOf(slot.get("jersey")),
                    x, y,
                    photoBytes(slot.get("photoUrl") == null ? null : String.valueOf(slot.get("photoUrl"))),
                    Boolean.TRUE.equals(slot.get("captain")),
                    "GK".equals(code));
            if ("SUBSTITUTE".equals(String.valueOf(slot.get("selection")))) {
                bench.add(token);
            } else if ("STARTING".equals(String.valueOf(slot.get("selection")))) {
                tokens.add(token);
            }
        }
        String opponent = match == null ? "Opponent" : String.valueOf(match.get("opponent"));
        String when = kigali(match == null ? null : match.get("kickoff"), lang);
        String venue = match == null || match.get("venue") == null ? "" : String.valueOf(match.get("venue"));
        String competition = match == null || match.get("competition") == null ? "" : String.valueOf(match.get("competition"));
        Integer squadSize = lineup == null || !(lineup.get("squadSize") instanceof Number number) ? null : number.intValue();
        if (!png) {
            return LineupSheetArt.pdf(opponent, when, venue, competition, formation, squadSize, tokens, bench, lang, "Viking FC");
        }
        byte[] logo = readLogo();
        byte[] crest = photoBytes(match == null || match.get("opponentLogo") == null ? null : String.valueOf(match.get("opponentLogo")));
        int height = "story".equals(ratio) ? 1920 : "square".equals(ratio) ? 1080 : 1350;
        return LineupSheetArt.png("vs " + opponent, when + "  ·  " + venue, formation, tokens, bench, logo, crest, template, 1080, height);
    }

    private String kigali(Object value, String lang) {
        if (value == null) {
            return "";
        }
        Instant instant = value instanceof Instant time ? time : Instant.parse(String.valueOf(value));
        return KigaliClock.format(instant, lang);
    }

    private byte[] readLogo() {
        try (var stream = LineupCenterController.class.getResourceAsStream("/brand/logo.png")) {
            return stream == null ? null : stream.readAllBytes();
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] photoBytes(String url) {
        if (url == null || !url.startsWith("/files/") || properties.uploads() == null) {
            return null;
        }
        try {
            Path root = Path.of(properties.uploads().dir()).toAbsolutePath().normalize();
            Path file = root.resolve(url.substring("/files/".length())).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (Exception ex) {
            return null;
        }
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

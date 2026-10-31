package rw.vikingfc.club.contract;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.api.PageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/captains")
@RequiredArgsConstructor
@Tag(name = "Captains")
public class CaptainController {

    private final CaptainService captainService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CaptainDtos.View>> list(@RequestParam(required = false) Long season) {
        return ApiResponse.ok("Captains", captainService.list(season));
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CaptainDtos.Summary> summary(@RequestParam(required = false) Long season) {
        return ApiResponse.ok("Captain summary", captainService.summary(season));
    }

    @GetMapping("/seasons")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CaptainDtos.SeasonOption>> seasons() {
        return ApiResponse.ok("Seasons", captainService.seasons());
    }

    @GetMapping(value = "/export", produces = "application/pdf")
    @PreAuthorize("isAuthenticated()")
    public org.springframework.http.ResponseEntity<byte[]> export(@RequestParam(required = false) Long season,
                                                                  @RequestParam(required = false) String lang) {
        String name = captainService.exportName(season);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .body(captainService.export(season, lang));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<CaptainDtos.View> assign(@Valid @RequestBody CaptainDtos.Assign request) {
        return ApiResponse.ok("Captain assigned", captainService.assign(request.playerId(), request.rank()));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<CaptainDtos.View>> history(
            @RequestParam(required = false) Long season,
            @RequestParam(required = false) CaptainRank rank,
            @RequestParam(defaultValue = "false") boolean current,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "startedAt") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok("Captain history", captainService.history(season, rank, current, q, sort, dir, page, size));
    }

    @PutMapping("/{rank}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<CaptainDtos.View> assignRank(@PathVariable CaptainRank rank, @RequestBody java.util.Map<String, Object> body) {
        Object raw = body.get("playerId");
        Long playerId = raw instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(raw));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        return ApiResponse.ok("Captain assigned", captainService.assign(playerId, rank, note));
    }

    @DeleteMapping("/{rank:[A-Z_]+}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Void> removeByRank(@PathVariable CaptainRank rank) {
        captainService.removeRank(rank);
        return ApiResponse.ok("Captain removed", null);
    }

    @DeleteMapping("/rank/{rank}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Void> removeRank(@PathVariable CaptainRank rank) {
        captainService.removeRank(rank);
        return ApiResponse.ok("Captain removed", null);
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        captainService.remove(id);
        return ApiResponse.ok("Captain removed", null);
    }
}

package rw.vikingfc.club.sanction;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.finance.PayMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
@Tag(name = "Fines")
public class FineController {

    private final FineService fineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) Long playerId,
                                                       @RequestParam(required = false) String players,
                                                       @RequestParam(defaultValue = "all") String preset,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String from,
                                                       @RequestParam(required = false) String to) {
        LocalDate today = LocalDate.now();
        LocalDate start = switch (preset == null ? "all" : preset) {
            case "today" -> today;
            case "week" -> today.minusDays(today.getDayOfWeek().getValue() - 1L);
            case "month" -> today.withDayOfMonth(1);
            case "year" -> today.withDayOfYear(1);
            case "custom" -> from == null || from.isBlank() ? null : LocalDate.parse(from);
            default -> null;
        };
        LocalDate end = "custom".equals(preset) && to != null && !to.isBlank() ? LocalDate.parse(to) : "all".equals(preset) ? null : today;
        Set<String> ids = players == null || players.isBlank() ? Set.of() : Set.of(players.split(","));
        List<Map<String, Object>> rows = fineService.ledger().stream().filter(row -> {
            if (playerId != null && !playerId.equals(row.get("playerId"))) return false;
            if (!ids.isEmpty() && !ids.contains(String.valueOf(row.get("playerId")))) return false;
            if (status != null && !status.isBlank() && !status.equals(String.valueOf(row.get("status")))) return false;
            if (start != null && row.get("date") != null && LocalDate.parse(String.valueOf(row.get("date"))).isBefore(start)) return false;
            return end == null || row.get("date") == null || !LocalDate.parse(String.valueOf(row.get("date"))).isAfter(end);
        }).toList();
        return ApiResponse.ok("Fines", rows);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok("Fines", fineService.summary());
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<List<Map<String, Object>>> ledger() {
        return ApiResponse.ok("Absence and fine ledger", fineService.ledger());
    }

    @GetMapping("/weeks")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<List<Map<String, Object>>> weeks() {
        return ApiResponse.ok("Fines by week", fineService.byWeek());
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> pay(@PathVariable Long id, @RequestBody Pay body) {
        Fine fine = fineService.pay(id, body.amount(), body.method(), body.reference());
        return ApiResponse.ok("Fine payment recorded", Map.of("id", fine.getId(), "status", fine.getStatus(), "balance", fine.balance()));
    }

    @PostMapping("/{id}/waive")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> waive(@PathVariable Long id, @RequestBody Waive body) {
        Fine fine = fineService.waive(id, body.reason());
        return ApiResponse.ok("Fine waived", Map.of("id", fine.getId(), "status", fine.getStatus()));
    }

    @PostMapping("/{id}/deduct")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> deduct(@PathVariable Long id) {
        return ApiResponse.ok("Fine deducted from the next instalment", fineService.deductFromPay(id));
    }

    public record Pay(@NotNull BigDecimal amount, @NotNull PayMethod method, String reference) {
    }

    public record Waive(@NotNull String reason) {
    }
}

package rw.vikingfc.club.bonus;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bonuses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
@Tag(name = "Bonuses")
public class BonusController {

    private final BonusService bonusService;

    @GetMapping("/types")
    public ApiResponse<BonusType[]> types() {
        return ApiResponse.ok("Bonus types", BonusType.values());
    }

    @GetMapping("/rules")
    public ApiResponse<List<Map<String, Object>>> rules() {
        return ApiResponse.ok("Bonus rules", bonusService.rules());
    }

    @PostMapping("/rules")
    public ApiResponse<Map<String, Object>> rule(@RequestBody Map<String, Object> body) {
        Long playerId = body.get("playerId") == null ? null : ((Number) body.get("playerId")).longValue();
        return ApiResponse.ok("Bonus rule saved", bonusService.createRule(
                playerId, BonusType.valueOf(String.valueOf(body.get("type"))),
                new BigDecimal(body.get("amount").toString()),
                body.get("validFrom") == null ? null : LocalDate.parse(body.get("validFrom").toString()),
                body.get("validTo") == null ? null : LocalDate.parse(body.get("validTo").toString()),
                body.get("description") == null ? null : body.get("description").toString()));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) BonusStatus status,
                                                        @RequestParam(required = false) Long playerId) {
        return ApiResponse.ok("Bonuses", bonusService.list(status, playerId));
    }

    @GetMapping("/liability")
    public ApiResponse<Map<String, Object>> liability() {
        return ApiResponse.ok("Bonus liability", bonusService.liability());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> manual(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Bonus created", bonusService.manual(
                ((Number) body.get("playerId")).longValue(),
                BonusType.valueOf(String.valueOf(body.get("type"))),
                new BigDecimal(body.get("amount").toString()),
                body.get("description") == null ? null : body.get("description").toString(),
                body.get("earnedOn") == null ? null : LocalDate.parse(body.get("earnedOn").toString())));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        return ApiResponse.ok("Bonus approved", bonusService.approve(id));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<Map<String, Object>> pay(@PathVariable Long id) {
        return ApiResponse.ok("Bonus paid", bonusService.pay(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Bonus cancelled", bonusService.cancel(id));
    }
}

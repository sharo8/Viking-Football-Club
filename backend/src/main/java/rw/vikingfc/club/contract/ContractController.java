package rw.vikingfc.club.contract;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.util.ClubDates;
import rw.vikingfc.club.settings.SettingsService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Tag(name = "Contracts")
public class ContractController {

    private final PlayerContractRepository contractRepository;
    private final SettingsService settingsService;
    private final Clock clock;

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('EXECUTIVE')")
    @Transactional(readOnly = true)
    public ApiResponse<List<Expiring>> expiring(@RequestParam(defaultValue = "6") int withinMonths) {
        LocalDate today = LocalDate.now(clock);
        LocalDate limit = today.plusMonths(withinMonths);
        Long seasonId = settingsService.currentSeason().getId();
        List<Expiring> rows = contractRepository.findBySeasonIdAndActiveTrue(seasonId).stream()
                .filter(contract -> !contract.getEndDate().isAfter(limit))
                .map(contract -> new Expiring(contract.getPlayer().getId(), contract.getPlayer().getFullName(),
                        contract.getEndDate(), ClubDates.remainingMonths(today, contract.getEndDate()),
                        contract.getReleaseClauseAmount(), contract.isReleaseClauseActive()))
                .toList();
        return ApiResponse.ok("Expiring contracts", rows);
    }

    public record Expiring(Long playerId, String fullName, LocalDate endDate, int remainingMonths,
                           java.math.BigDecimal releaseClauseAmount, boolean releaseClauseActive) {
    }
}

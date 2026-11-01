package rw.vikingfc.club.finance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
@Tag(name = "Finance")
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/summary")
    @Operation(summary = "Workbook summary tables A to H, plus staff and club payroll")
    public ApiResponse<FinanceDtos.Summary> summary() {
        return ApiResponse.ok("Finance summary", financeService.summary());
    }

    @GetMapping("/roster")
    @Operation(summary = "One payroll row per person, split into players, staff and administration")
    public ApiResponse<Map<String, Object>> roster() {
        return ApiResponse.ok("Payroll roster", financeService.roster());
    }

    @GetMapping("/unpaid")
    public ApiResponse<List<FinanceDtos.InstalmentView>> unpaid() {
        return ApiResponse.ok("Unpaid instalments", financeService.unpaid());
    }

    @GetMapping("/overdue")
    public ApiResponse<List<FinanceDtos.InstalmentView>> overdue() {
        return ApiResponse.ok("Overdue instalments", financeService.overdue());
    }

    @GetMapping("/players/{id}/statement")
    public ApiResponse<FinanceDtos.Statement> statement(@PathVariable Long id) {
        return ApiResponse.ok("Player statement", financeService.statement(id));
    }

    @PostMapping("/instalments/{id}/payments")
    public ApiResponse<FinanceDtos.InstalmentView> pay(@PathVariable Long id, @Valid @RequestBody FinanceDtos.PayRequest request) {
        return ApiResponse.ok("Payment recorded", financeService.record(id, request));
    }

    @PostMapping("/payments/{id}/reverse")
    public ApiResponse<FinanceDtos.InstalmentView> reverse(@PathVariable Long id, @Valid @RequestBody FinanceDtos.ReverseRequest request) {
        return ApiResponse.ok("Payment reversed", financeService.reverse(id, request.reason()));
    }
}

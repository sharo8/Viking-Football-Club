package rw.vikingfc.club.partner;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
@Tag(name = "Partners")
public class PartnerController {

    private final PartnerService partnerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Partners", partnerService.list());
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('EXECUTIVE','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> report() {
        return ApiResponse.ok("Partner report", partnerService.report());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','MARKETING_MANAGER')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody PartnerService.PartnerWrite request) {
        return ApiResponse.ok("Partner saved", partnerService.create(request));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> schedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Partner invoice added", partnerService.addPayment(id,
                LocalDate.parse(body.get("dueDate")), new BigDecimal(body.get("amount"))));
    }

    @PostMapping("/payments/{paymentId}/pay")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> pay(@PathVariable Long paymentId, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Partner payment recorded", partnerService.recordPayment(paymentId,
                new BigDecimal(body.get("amount")), body.get("reference")));
    }
}

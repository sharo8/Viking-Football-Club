package rw.vikingfc.club.bonus;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

@RestController
@RequestMapping("/api/bonus-types")
public class BonusTypeController {

    @GetMapping
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<BonusType[]> types() {
        return ApiResponse.ok("Bonus types", BonusType.values());
    }
}

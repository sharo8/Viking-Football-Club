package rw.vikingfc.club.transfer;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import rw.vikingfc.club.player.Position;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers")
public class TransferController {

    private final TransferService transferService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Transfers", transferService.list());
    }

    @GetMapping("/totals")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> totals() {
        return ApiResponse.ok("Transfer totals", transferService.totals());
    }

    @PostMapping
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody TransferService.TransferWrite request) {
        return ApiResponse.ok("Transfer opened", transferService.create(request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> complete(@PathVariable Long id,
                                                      @RequestParam(required = false) Position position,
                                                      @RequestParam(required = false) Integer jerseyNumber) {
        return ApiResponse.ok("Transfer completed", transferService.complete(id, position, jerseyNumber));
    }
}

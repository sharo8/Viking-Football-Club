package rw.vikingfc.club.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.season.Season;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    @Operation(summary = "Current season rules, rates and contract defaults")
    public ApiResponse<SettingsDtos.View> current() {
        return ApiResponse.ok("Settings", settingsService.view());
    }

    @PutMapping
    @PreAuthorize("hasRole('EXECUTIVE')")
    @Operation(summary = "Update the current season settings")
    public ApiResponse<SettingsDtos.View> update(@Valid @RequestBody SettingsDtos.Update request) {
        return ApiResponse.ok("Settings updated", settingsService.update(request));
    }

    @GetMapping("/season")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Season> season() {
        return ApiResponse.ok("Season", settingsService.currentSeason());
    }

    @GetMapping("/positions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PositionLineMapping>> positions() {
        return ApiResponse.ok("Positions", settingsService.mappings());
    }

    @GetMapping("/salary-bands")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<List<SalaryBand>> bands() {
        return ApiResponse.ok("Salary bands", settingsService.bands());
    }

    @GetMapping("/staff-roles")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<List<StaffRoleDepartment>> roles() {
        return ApiResponse.ok("Staff roles", settingsService.staffRoles());
    }

    @GetMapping("/fine-types")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<List<FineTypeConfig>> fines() {
        return ApiResponse.ok("Fine types", settingsService.fineTypes());
    }

    @PutMapping("/fine-types/{id}")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<FineTypeConfig> updateFine(@PathVariable Long id, @Valid @RequestBody SettingsDtos.FineTypeUpdate request) {
        return ApiResponse.ok("Fine type updated", settingsService.updateFineType(id, request));
    }
}

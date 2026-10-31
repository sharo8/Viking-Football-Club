package rw.vikingfc.club.staff;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
@Tag(name = "Staff")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Staff", staffService.list());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody StaffService.StaffWrite request) {
        return ApiResponse.ok("Staff member created", staffService.create(request));
    }
}

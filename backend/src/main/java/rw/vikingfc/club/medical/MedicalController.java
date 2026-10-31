package rw.vikingfc.club.medical;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medical")
@RequiredArgsConstructor
@Tag(name = "Medical")
public class MedicalController {

    private final MedicalService medicalService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('EXECUTIVE','DOCTOR')")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok("Medical dashboard", medicalService.dashboard());
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('EXECUTIVE','DOCTOR','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<List<Map<String, Object>>> availability(@RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok("Availability", medicalService.availability(date));
    }

    @PostMapping("/injuries")
    @PreAuthorize("hasAnyRole('EXECUTIVE','DOCTOR')")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody MedicalService.InjuryWrite request) {
        return ApiResponse.ok("Injury registered", medicalService.register(request));
    }

    @PutMapping("/injuries/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','DOCTOR')")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        InjuryStatus status = InjuryStatus.valueOf(body.get("status"));
        LocalDate expected = body.get("expectedReturnDate") == null || body.get("expectedReturnDate").isBlank()
                ? null : LocalDate.parse(body.get("expectedReturnDate"));
        return ApiResponse.ok("Injury updated", medicalService.updateStatus(id, status, expected, body.get("note")));
    }
}

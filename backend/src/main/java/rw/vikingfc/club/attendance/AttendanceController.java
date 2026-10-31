package rw.vikingfc.club.attendance;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','DOCTOR')")
    public ApiResponse<Map<String, Object>> kpis(
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) String weekday,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Long playerId) {
        return ApiResponse.ok("Attendance", attendanceService.kpis(week, weekday, date, position, playerId));
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','DOCTOR')")
    public ApiResponse<Map<String, Object>> matrix() {
        return ApiResponse.ok("Attendance matrix", attendanceService.matrix());
    }

    @GetMapping("/weeks")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH','DOCTOR')")
    public ApiResponse<List<Map<String, Object>>> weeks() {
        return ApiResponse.ok("Weekly summary", attendanceService.weekly());
    }

    @GetMapping("/at-risk")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<List<Map<String, Object>>> atRisk() {
        return ApiResponse.ok("Players at risk", attendanceService.atRisk());
    }

    @PostMapping("/sessions/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
    public ApiResponse<List<Map<String, Object>>> save(@PathVariable Long id, @Valid @RequestBody AttendanceDtos.Bulk request) {
        return ApiResponse.ok("Attendance saved", attendanceService.saveBulk(id, request.marks()));
    }
}

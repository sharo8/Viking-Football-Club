package rw.vikingfc.club.attendance;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.common.api.ApiResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance desk")
@PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','ASSISTANT_COACH','GOALKEEPER_COACH')")
public class AttendanceDeskController {

    private final AttendanceDeskService desk;

    @GetMapping("/sessions")
    public ApiResponse<Map<String, Object>> sessions(
            @RequestParam(defaultValue = "players") String audience,
            @RequestParam(defaultValue = "month") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok("Sessions", desk.sessions(audience, preset, from, to, kind, status, q, page, size));
    }

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody AttendanceDeskDtos.SessionWrite write) {
        return ApiResponse.ok("Session opened", desk.create(write));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok("Session", desk.detail(id));
    }

    @PutMapping("/sessions/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody AttendanceDeskDtos.SessionWrite write) {
        return ApiResponse.ok("Session updated", desk.update(id, write));
    }

    @PutMapping("/sessions/{id}/records")
    public ApiResponse<Map<String, Object>> records(@PathVariable Long id, @Valid @RequestBody AttendanceDeskDtos.MarksWrite write) {
        return ApiResponse.ok("Attendance saved", desk.saveMarks(id, write));
    }

    @PostMapping("/sessions/{id}/close")
    public ApiResponse<Map<String, Object>> close(@PathVariable Long id) {
        return ApiResponse.ok("Session closed", desk.close(id));
    }

    @PostMapping("/sessions/{id}/reopen")
    public ApiResponse<Map<String, Object>> reopen(@PathVariable Long id, @Valid @RequestBody AttendanceDeskDtos.ReasonWrite write) {
        return ApiResponse.ok("Session reopened", desk.reopen(id, write.reason()));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @Valid @RequestBody AttendanceDeskDtos.ReasonWrite write) {
        desk.delete(id, write.reason());
        return ApiResponse.ok("Session deleted", null);
    }

    @GetMapping("/grid")
    public ApiResponse<Map<String, Object>> grid(
            @RequestParam(defaultValue = "players") String audience,
            @RequestParam(defaultValue = "month") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String grain,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String mark,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String sessionStatus,
            @RequestParam(required = false) Boolean hasFine,
            @RequestParam(required = false) Boolean edited,
            @RequestParam(required = false) String recordedBy,
            @RequestParam(required = false) String players) {
        return ApiResponse.ok("Attendance grid", desk.grid(audience, preset, from, to, grain, q, position, line, role, department, mark, kind, sessionStatus, hasFine, edited, recordedBy, players));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(
            @RequestParam(defaultValue = "players") String audience,
            @RequestParam(defaultValue = "month") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String grain,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String mark,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String sessionStatus,
            @RequestParam(required = false) Boolean hasFine,
            @RequestParam(required = false) Boolean edited,
            @RequestParam(required = false) String recordedBy,
            @RequestParam(required = false) String compare) {
        return ApiResponse.ok("Attendance statistics", desk.stats(audience, preset, from, to, grain, q, position, line, role, department, mark, kind, sessionStatus, hasFine, edited, recordedBy, compare));
    }

    @GetMapping("/people/{type}/{id}/summary")
    public ApiResponse<Map<String, Object>> summary(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Attendance summary", desk.summary(type, id, from, to));
    }

    @GetMapping("/people/{type}/{id}/records")
    public ApiResponse<List<Map<String, Object>>> personRecords(@PathVariable String type, @PathVariable Long id) {
        return ApiResponse.ok("Attendance records", desk.records(type, id));
    }

    @GetMapping("/people/{type}/{id}/export")
    public ResponseEntity<byte[]> personExport(@PathVariable String type, @PathVariable Long id,
                                               @RequestParam(defaultValue = "pdf") String format,
                                               @RequestParam(defaultValue = "en") String lang) {
        return file(desk.personExport(type, id, format, lang), "attendance-" + id + "." + ("pdf".equals(format) ? "pdf" : "xlsx"), format);
    }

    @PostMapping("/justifications")
    public ApiResponse<Map<String, Object>> justify(@Valid @RequestBody AttendanceDeskDtos.JustificationWrite write) {
        return ApiResponse.ok("Justification saved", desk.justify(write));
    }

    @GetMapping("/matches")
    public ApiResponse<List<Map<String, Object>>> matches() {
        return ApiResponse.ok("Matches", desk.matches());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(defaultValue = "en") String lang,
            @RequestParam(defaultValue = "players") String audience,
            @RequestParam(defaultValue = "month") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String grain,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String mark,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String sessionStatus,
            @RequestParam(required = false) Boolean hasFine,
            @RequestParam(required = false) Boolean edited,
            @RequestParam(required = false) String recordedBy) {
        byte[] body = desk.export(format, lang, audience, preset, from, to, grain, q, position, line, role, department, mark, kind, sessionStatus, hasFine, edited, recordedBy);
        String ext = "pdf".equals(format) ? "pdf" : "csv".equals(format) ? "csv" : "xlsx";
        return file(body, "attendance-" + audience + "." + ext, format);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam(defaultValue = "players") String audience) {
        return file(desk.template(audience), "attendance-template-" + audience + ".xlsx", "xlsx");
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> preview(@RequestPart("file") MultipartFile file,
                                                    @RequestParam(defaultValue = "players") String audience,
                                                    @RequestParam(defaultValue = "TRAINING") String kind) {
        return ApiResponse.ok("Import preview", desk.preview(file, audience, kind));
    }

    @PostMapping("/import/commit")
    public ApiResponse<Map<String, Object>> commit(@Valid @RequestBody AttendanceDeskDtos.ImportCommit commit) {
        return ApiResponse.ok("Attendance imported", desk.commit(commit));
    }

    private ResponseEntity<byte[]> file(byte[] body, String name, String format) {
        MediaType type = switch (format) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "csv" -> MediaType.parseMediaType("text/csv");
            default -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(type)
                .body(body);
    }
}

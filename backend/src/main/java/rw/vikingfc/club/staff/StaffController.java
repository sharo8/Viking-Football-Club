package rw.vikingfc.club.staff;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.report.DirectorySheets;
import rw.vikingfc.club.common.util.SecurityUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
@Tag(name = "Staff")
public class StaffController {

    private final StaffService staffService;
    private final StaffAccountService accounts;
    private final DirectorySheets sheets;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Staff", staffService.list());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody StaffService.StaffWrite request) {
        return ApiResponse.ok("Staff member created", staffService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody StaffService.StaffWrite request) {
        return ApiResponse.ok("Staff member updated", staffService.update(id, request));
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> photo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Photo saved", staffService.photo(id, file));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String format, @RequestParam(defaultValue = "en") String lang, @RequestParam(required = false) String q) {
        List<String> headers = "fr".equals(lang)
                ? List.of("Nom", "Rôle", "Département", "Téléphone", "E-mail", "Statut")
                : "rw".equals(lang)
                ? List.of("Izina", "Uruhare", "Ishami", "Telefoni", "Imeri", "Imiterere")
                : List.of("Name", "Role", "Department", "Phone", "Email", "Status");
        String title = "fr".equals(lang) ? "Personnel" : "rw".equals(lang) ? "Abakozi" : "Staff";
        return file(format, title, headers, staffService.table(q), lang);
    }

    private ResponseEntity<byte[]> file(String format, String title, List<String> headers, List<List<String>> rows, String lang) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        var principal = SecurityUtils.principal();
        String who = principal == null ? SecurityUtils.email() : principal.getFullName();
        byte[] body = pdf ? sheets.pdf(title, who, headers, rows, lang) : sheets.excel(title, who, headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=staff." + (pdf ? "pdf" : "xlsx"))
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping("/{id}/account")
    public ApiResponse<Map<String, Object>> account(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var staff = staffService.load(id);
        accounts.open(staff, body.get("email"), !"false".equalsIgnoreCase(body.get("sendInvitation")), body.get("language"));
        return ApiResponse.ok("Account created", staffService.get(id));
    }

    @PostMapping("/{id}/resend-invitation")
    public ApiResponse<Map<String, Object>> resend(@PathVariable Long id) {
        accounts.resend(id);
        return ApiResponse.ok("Invitation sent", staffService.get(id));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Map<String, Object>> reset(@PathVariable Long id) {
        accounts.resetPassword(id);
        return ApiResponse.ok("Password reset", staffService.get(id));
    }

    @PostMapping("/{id}/suspend")
    public ApiResponse<Map<String, Object>> suspend(@PathVariable Long id) {
        accounts.suspend(id);
        return ApiResponse.ok("Account suspended", staffService.get(id));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<Map<String, Object>> activate(@PathVariable Long id) {
        accounts.activate(id);
        return ApiResponse.ok("Account activated", staffService.get(id));
    }

    @DeleteMapping("/{id}/account")
    public ApiResponse<Map<String, Object>> removeAccount(@PathVariable Long id, @RequestBody Map<String, String> body) {
        accounts.remove(id, body.get("reasonCode"), body.get("reason"));
        return ApiResponse.ok("Account removed", staffService.get(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> archive(@PathVariable Long id) {
        throw rw.vikingfc.club.common.exception.BusinessException.badRequest("A reason is required", "REASON_REQUIRED");
    }
}

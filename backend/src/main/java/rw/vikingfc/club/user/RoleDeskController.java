package rw.vikingfc.club.user;

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
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.report.DirectorySheets;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
public class RoleDeskController {

    private final RoleDeskService desk;
    private final DirectorySheets sheets;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok("Roles", desk.list());
    }

    @GetMapping("/cleanup")
    public ApiResponse<List<Map<String, Object>>> cleanup() {
        return ApiResponse.ok("Cleanup", desk.cleanup());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String format, @RequestParam(defaultValue = "en") String lang, @RequestParam(required = false) String q) {
        List<String> headers = "fr".equals(lang)
                ? List.of("Nom", "Catégorie", "OTP", "Comptes")
                : "rw".equals(lang) ? List.of("Izina", "Icyiciro", "OTP", "Konti") : List.of("Name", "Category", "OTP", "Accounts");
        String title = "fr".equals(lang) ? "Rôles" : "rw".equals(lang) ? "Inshingano" : "Roles";
        boolean pdf = "pdf".equalsIgnoreCase(format);
        var principal = SecurityUtils.principal();
        String who = principal == null ? SecurityUtils.email() : principal.getFullName();
        var rows = desk.table(q, lang);
        byte[] body = pdf ? sheets.pdf(title, who, headers, rows, lang) : sheets.excel(title, who, headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=roles." + (pdf ? "pdf" : "xlsx"))
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Role created", desk.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Role updated", desk.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        desk.delete(id);
        return ApiResponse.ok("Role deleted", null);
    }

    @PostMapping("/{id}/reassign-and-delete")
    public ApiResponse<Void> reassign(@PathVariable Long id, @RequestBody Map<String, String> body) {
        desk.reassignAndDelete(id, body.get("targetCode"));
        return ApiResponse.ok("Users moved and the role was deleted", null);
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<Map<String, Object>> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Number userId = (Number) body.get("userId");
        return ApiResponse.ok("Role assigned", desk.assign(id, userId.longValue()));
    }
}

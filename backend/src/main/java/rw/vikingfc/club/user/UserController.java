package rw.vikingfc.club.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.report.DirectorySheets;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;
    private final DirectorySheets sheets;

    @GetMapping
    @Operation(summary = "List club accounts")
    public ApiResponse<List<UserDtos.View>> list() {
        return ApiResponse.ok("Users", userService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one account")
    public ApiResponse<UserDtos.View> get(@PathVariable Long id) {
        return ApiResponse.ok("User", userService.get(id));
    }

    @PostMapping
    @Operation(summary = "Create an account with the temporary password")
    public ApiResponse<UserDtos.Credentials> create(@Valid @RequestBody UserDtos.CreateRequest request) {
        return ApiResponse.ok("User created. Share the temporary password once; it is not stored in clear text.",
                userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update name, role or active flag")
    public ApiResponse<UserDtos.View> update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateRequest request) {
        return ApiResponse.ok("User updated", userService.update(id, request));
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserDtos.View> photo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Photo saved", userService.photo(id, file));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String format, @RequestParam(defaultValue = "en") String lang, @RequestParam(required = false) String q) {
        List<String> headers = "fr".equals(lang)
                ? List.of("Nom", "E-mail", "Rôle", "Statut")
                : "rw".equals(lang) ? List.of("Izina", "Imeri", "Uruhare", "Imiterere") : List.of("Name", "Email", "Role", "Status");
        String title = "fr".equals(lang) ? "Administration" : "rw".equals(lang) ? "Ubuyobozi" : "Administration";
        boolean pdf = "pdf".equalsIgnoreCase(format);
        var principal = SecurityUtils.principal();
        String who = principal == null ? SecurityUtils.email() : principal.getFullName();
        var rows = userService.table(q, lang);
        byte[] body = pdf ? sheets.pdf(title, who, headers, rows, lang) : sheets.excel(title, who, headers, rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=administration." + (pdf ? "pdf" : "xlsx"))
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable an account")
    public ApiResponse<UserDtos.View> disable(@PathVariable Long id) {
        return ApiResponse.ok("User disabled", userService.disable(id));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "Enable an account and clear the lock")
    public ApiResponse<UserDtos.View> enable(@PathVariable Long id) {
        return ApiResponse.ok("User enabled", userService.enable(id));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset the password and return the temporary password once")
    public ApiResponse<UserDtos.Credentials> reset(@PathVariable Long id) {
        return ApiResponse.ok("Password reset. The temporary password is shown once.", userService.resetPassword(id));
    }
}

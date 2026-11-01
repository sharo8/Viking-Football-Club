package rw.vikingfc.club.player;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
import rw.vikingfc.club.bonus.BonusService;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.api.PageResponse;
import rw.vikingfc.club.match.AnalyticsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Tag(name = "Players")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerOfficeService officeService;
    private final BirthdayService birthdayService;
    private final AnalyticsService analyticsService;
    private final BonusService bonusService;
    private final PlayerImportService importService;
    private final PlayerAccountService playerAccountService;
    private final rw.vikingfc.club.report.DirectorySheets sheets;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Squad register. Pay fields are returned only to the executive tier.")
    public ApiResponse<List<?>> list() {
        return ApiResponse.ok("Players", playerService.list());
    }

    @GetMapping("/data-warnings")
    @PreAuthorize("hasRole('EXECUTIVE')")
    @Operation(summary = "Duplicate jerseys and advances larger than the net 3-month entitlement")
    public ApiResponse<List<PlayerDtos.Warning>> warnings() {
        return ApiResponse.ok("Data warnings", playerService.warnings());
    }

    @GetMapping("/new-signings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Recent signings, with photo when one has been uploaded")
    public ApiResponse<List<PlayerDtos.Public>> signings() {
        return ApiResponse.ok("New signings", playerService.newSignings());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Object> get(@PathVariable Long id) {
        return ApiResponse.ok("Player", playerService.get(id));
    }

    @PostMapping("/{id}/account")
    @PreAuthorize("hasAnyRole('EXECUTIVE','HEAD_COACH','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> account(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Player login created", playerAccountService.open(id, body.get("email")));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Season statistics: minutes, goals, assists, attendance and fines")
    public ApiResponse<java.util.Map<String, Object>> stats(@PathVariable Long id) {
        return ApiResponse.ok("Player statistics", analyticsService.playerStats(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Object> create(@Valid @RequestBody PlayerDtos.Write request) {
        return ApiResponse.ok("Player created", playerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Object> update(@PathVariable Long id, @Valid @RequestBody PlayerDtos.Write request) {
        return ApiResponse.ok("Player updated", playerService.update(id, request));
    }

    @GetMapping("/directory")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<Map<String, Object>>> directory(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String birthday,
            @RequestParam(defaultValue = "false") boolean missing,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "false") boolean expiring,
            @RequestParam(defaultValue = "jersey") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.ok("Directory", officeService.directory(q, line, position, status, birthday, missing, archived, expiring, sort, dir, page, size));
    }

    @GetMapping("/counts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> counts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String birthday,
            @RequestParam(defaultValue = "false") boolean missing) {
        return ApiResponse.ok("Counts", officeService.counts(q, status, birthday, missing));
    }

    @GetMapping("/jersey-availability")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> jersey(@RequestParam(required = false) Integer number, @RequestParam(required = false) Long excludeId) {
        return ApiResponse.ok("Jersey", officeService.jerseyAvailability(number, excludeId));
    }

    @GetMapping("/{id}/bonus-rules")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<List<Map<String, Object>>> bonusRules(@PathVariable Long id) {
        return ApiResponse.ok("Bonus rules", bonusService.playerRules(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/bonus-rules")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<List<Map<String, Object>>> saveBonusRules(@PathVariable Long id, @RequestBody List<Map<String, Object>> rows) {
        return ApiResponse.ok("Bonus rules saved", bonusService.replacePlayerRules(id, rows));
    }

    @PostMapping(value = "/form", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Object> form(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok("Player saved", officeService.save(body));
    }

    @PostMapping(value = "/form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Object> formWithPhoto(@RequestPart("player") String json,
                                             @RequestPart(value = "photo", required = false) MultipartFile photo) throws java.io.IOException {
        Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        boolean hasPhoto = photo != null && !photo.isEmpty();
        Object saved = officeService.save(body, hasPhoto);
        if (hasPhoto) {
            Long id = saved instanceof PlayerDtos.Full full ? full.id()
                    : saved instanceof PlayerDtos.Operational operational ? operational.id()
                    : ((PlayerDtos.Public) saved).id();
            try {
                playerService.uploadPhoto(id, photo, PhotoType.PROFILE);
            } catch (RuntimeException ex) {
                if (body.get("id") == null) {
                    officeService.destroy(id);
                }
                throw ex;
            }
        }
        return ApiResponse.ok("Player saved", saved);
    }

    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public org.springframework.http.ResponseEntity<byte[]> template() {
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=viking-fc-players.xlsx")
                .body(importService.template());
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> previewImport(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Import preview", importService.preview(file));
    }

    @PostMapping("/import/{importId}/commit")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> commitImport(@PathVariable String importId) {
        return ApiResponse.ok("Import finished", importService.commit(importId));
    }

    @GetMapping("/import/{importId}/errors")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public org.springframework.http.ResponseEntity<byte[]> importErrors(@PathVariable String importId) {
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=rejected-players.csv")
                .body(importService.errors(importId));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<Map<String, Object>> importFile(@RequestPart("file") MultipartFile file,
                                                       @RequestParam(defaultValue = "false") boolean confirm) throws java.io.IOException {
        return ApiResponse.ok(confirm ? "Import finished" : "Import preview", officeService.importCsv(new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8), confirm));
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public org.springframework.http.ResponseEntity<?> export(@RequestParam(defaultValue = "csv") String format,
                                                             @RequestParam(defaultValue = "en") String lang) {
        if (!"pdf".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return org.springframework.http.ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=viking-fc-players.csv")
                    .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                    .body(officeService.exportCsv());
        }
        java.util.List<String> headers = "fr".equals(lang)
                ? java.util.List.of("Nom", "Maillot", "Poste", "Nationalité", "Téléphone", "Statut")
                : "rw".equals(lang)
                ? java.util.List.of("Izina", "Umupira", "Umwanya", "Ubwenegihugu", "Telefoni", "Imiterere")
                : java.util.List.of("Name", "Jersey", "Position", "Nationality", "Phone", "Status");
        String title = "fr".equals(lang) ? "Joueurs" : "rw".equals(lang) ? "Abakinnyi" : "Players";
        boolean pdf = "pdf".equalsIgnoreCase(format);
        var principal = rw.vikingfc.club.common.util.SecurityUtils.principal();
        String who = principal == null ? rw.vikingfc.club.common.util.SecurityUtils.email() : principal.getFullName();
        byte[] body = pdf
                ? sheets.pdf(title, who, headers, officeService.exportRows(), lang)
                : sheets.excel(title, who, headers, officeService.exportRows());
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=viking-fc-players." + (pdf ? "pdf" : "xlsx"))
                .contentType(pdf ? org.springframework.http.MediaType.APPLICATION_PDF : org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/analytics")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> analytics() {
        return ApiResponse.ok("Analytics", officeService.analytics());
    }

    @GetMapping("/birthdays")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> birthdays(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok("Birthdays", officeService.upcoming(days));
    }

    @PostMapping("/birthdays/run")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Integer> runBirthdays() {
        return ApiResponse.ok("Birthday job finished", officeService.runBirthdays(birthdayService));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> messages(@PathVariable Long id) {
        return ApiResponse.ok("Messages", officeService.messages(id));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> message(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Message queued", officeService.message(id, body.get("subject"), body.get("body"), body.getOrDefault("language", "en")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<String> archive(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean hard) {
        return ApiResponse.ok(hard ? officeService.destroy(id) : officeService.archive(id), id.toString());
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER')")
    public ApiResponse<String> restore(@PathVariable Long id) {
        return ApiResponse.ok(officeService.restore(id), id.toString());
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH','SOCIAL_MEDIA_MANAGER','MARKETING_MANAGER')")
    public ApiResponse<PlayerDtos.Photo> photo(@PathVariable Long id,
                                               @RequestPart("file") MultipartFile file,
                                               @RequestParam(defaultValue = "PROFILE") PhotoType type) {
        return ApiResponse.ok("Photo stored", playerService.uploadPhoto(id, file, type));
    }

    @GetMapping("/{id}/photos")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PlayerDtos.Photo>> photos(@PathVariable Long id) {
        return ApiResponse.ok("Photos", playerService.photos(id));
    }
}

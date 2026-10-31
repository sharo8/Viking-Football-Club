package rw.vikingfc.club.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.exception.NotFoundException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email/outbox")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailOutboxRepository repository;
    private final ClubMailService mailService;

    @GetMapping
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<List<EmailOutbox>> list(@RequestParam(required = false) String status) {
        List<EmailOutbox> rows = status == null || status.isBlank()
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);
        return ApiResponse.ok("Email log", rows);
    }

    @GetMapping("/diagnostics")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> diagnostics() {
        return ApiResponse.ok(String.valueOf(mailService.diagnostics().get("reason")), mailService.diagnostics());
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> test(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        if (to == null || to.isBlank()) {
            throw rw.vikingfc.club.common.exception.BusinessException.badRequest("A recipient is required");
        }
        var queued = mailService.enqueue(to, body.getOrDefault("subject", "Viking FC test"),
                mailService.layout("Test", "<p>" + body.getOrDefault("body", "Test") + "</p>"),
                "test", "en", "TEST", "office");
        mailService.dispatch(queued.getId());
        return ApiResponse.ok(mailService.deliveryNote(), mailService.diagnostics());
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Long> retry(@PathVariable Long id) {
        EmailOutbox row = repository.findById(id).orElseThrow(() -> NotFoundException.of("Email", id));
        row.setStatus("QUEUED");
        row.setAttempts(0);
        repository.save(row);
        mailService.dispatch(row.getId());
        return ApiResponse.ok("Retry queued", row.getId());
    }
}

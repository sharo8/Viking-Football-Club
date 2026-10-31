package rw.vikingfc.club.audit;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.common.api.PageResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
@Tag(name = "Audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> list(Pageable pageable) {
        return ApiResponse.ok("Audit log", PageResponse.of(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(log -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", log.getId());
            body.put("actor", log.getActorEmail());
            body.put("role", log.getActorRole());
            body.put("action", log.getAction());
            body.put("entity", log.getEntityName());
            body.put("entityId", log.getEntityId());
            body.put("before", log.getBeforeJson());
            body.put("after", log.getAfterJson());
            body.put("ip", log.getIpAddress());
            body.put("at", log.getCreatedAt());
            return body;
        })));
    }
}

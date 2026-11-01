package rw.vikingfc.club.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rw.vikingfc.club.common.util.SecurityUtils;
import rw.vikingfc.club.security.UserPrincipal;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void record(String action, String entity, Object id, Object before, Object after, String ip) {
        UserPrincipal principal = SecurityUtils.principal();
        recordFor(
                SecurityUtils.email(),
                principal == null ? null : principal.getFullName(),
                principal == null ? null : principal.getRole().name(),
                action, entity, id, before, after, ip);
    }

    public void recordFor(String email, String name, String role, String action, String entity, Object id, Object before, Object after) {
        recordFor(email, name, role, action, entity, id, before, after, null);
    }

    public void recordFor(String email, String name, String role, String action, String entity, Object id, Object before, Object after, String ip) {
        AuditLog log = new AuditLog();
        log.setActorEmail(email);
        log.setActorName(name);
        log.setActorRole(role);
        log.setAction(action);
        log.setEntityName(entity);
        log.setEntityId(id == null ? null : String.valueOf(id));
        log.setBeforeJson(json(before));
        log.setAfterJson(json(after));
        log.setIpAddress(ip);
        auditLogRepository.save(log);
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String raw = objectMapper.writeValueAsString(value);
            return raw.length() > 4000 ? raw.substring(0, 4000) : raw;
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}

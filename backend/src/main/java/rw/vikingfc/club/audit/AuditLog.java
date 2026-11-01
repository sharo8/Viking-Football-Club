package rw.vikingfc.club.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
    @Column(length = 180)
    private String actorEmail;
    @Column(name = "actor_name", length = 160)
    private String actorName;
    @Column(length = 40)
    private String actorRole;
    @Column(nullable = false, length = 40)
    private String action;
    @Column(nullable = false, length = 80)
    private String entityName;
    @Column(length = 40)
    private String entityId;
    @Column(length = 4000)
    private String beforeJson;
    @Column(length = 4000)
    private String afterJson;
    @Column(length = 64)
    private String ipAddress;
}

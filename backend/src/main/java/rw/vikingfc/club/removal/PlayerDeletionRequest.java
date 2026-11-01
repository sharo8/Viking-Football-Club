package rw.vikingfc.club.removal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_deletion_requests")
public class PlayerDeletionRequest extends BaseEntity {
    @Column(nullable = false)
    private Long playerId;
    @Column(nullable = false, length = 2000)
    private String reason;
    @Column(nullable = false)
    private Long requestedBy;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false)
    private boolean hasHistory;
    @Column(length = 2000)
    private String historySummary;
    private Instant resolvedAt;
}

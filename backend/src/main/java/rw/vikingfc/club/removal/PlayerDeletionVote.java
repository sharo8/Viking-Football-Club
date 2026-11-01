package rw.vikingfc.club.removal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_deletion_votes", uniqueConstraints = @UniqueConstraint(name = "uq_deletion_vote", columnNames = {"request_id", "voter_role"}))
public class PlayerDeletionVote extends BaseEntity {
    @Column(nullable = false)
    private Long requestId;
    @Column(nullable = false)
    private Long voterUserId;
    @Column(nullable = false, length = 40)
    private String voterRole;
    @Column(nullable = false, length = 20)
    private String decision;
    @Column(length = 500)
    private String comment;
    @Column(nullable = false)
    private Instant decidedAt;
}

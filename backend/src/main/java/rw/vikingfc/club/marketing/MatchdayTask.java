package rw.vikingfc.club.marketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.match.FootballMatch;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "matchday_tasks")
public class MatchdayTask extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private FootballMatch match;
    @Column(nullable = false, length = 200)
    private String label;
    @Column(nullable = false)
    private boolean done;
    @Column(length = 40)
    private String ownerRole;
}

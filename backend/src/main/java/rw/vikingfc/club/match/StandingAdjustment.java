package rw.vikingfc.club.match;

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
import rw.vikingfc.club.competition.Competition;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "standing_adjustments")
public class StandingAdjustment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;
    @Column(nullable = false, length = 160)
    private String teamName;
    @Column(nullable = false)
    private int pointsDelta;
    @Column(nullable = false, length = 400)
    private String reason;
}

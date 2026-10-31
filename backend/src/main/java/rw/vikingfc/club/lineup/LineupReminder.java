package rw.vikingfc.club.lineup;

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
@Table(name = "lineup_reminders")
public class LineupReminder extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private FootballMatch match;
    @Column(nullable = false, length = 20)
    private String kind;
}

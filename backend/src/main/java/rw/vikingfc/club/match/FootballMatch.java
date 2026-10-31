package rw.vikingfc.club.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;
import rw.vikingfc.club.season.Season;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "matches")
public class FootballMatch extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(nullable = false, length = 160)
    private String competition;
    @Column(length = 80)
    private String roundLabel;
    private Integer matchday;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opponent_id", nullable = false)
    private Opponent opponent;
    @Column(nullable = false)
    private Instant kickoff;
    @Column(length = 160)
    private String venue;
    @Column(nullable = false)
    private boolean homeMatch;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;
    @Column(nullable = false)
    private int goalsFor;
    @Column(nullable = false)
    private int goalsAgainst;
    private Integer crowdAttendance;
    @Column(length = 160)
    private String referee;
    @Column(length = 2000)
    private String notes;
    @Column(nullable = false)
    private int stoppageMinutes;
    @Column(length = 20)
    private String formation;
}

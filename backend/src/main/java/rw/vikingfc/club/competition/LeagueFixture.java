package rw.vikingfc.club.competition;

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
import rw.vikingfc.club.match.MatchStatus;
import rw.vikingfc.club.match.Opponent;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "league_fixtures")
public class LeagueFixture extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;
    private Integer matchday;
    @Column(length = 40)
    private String stage;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false)
    private Opponent home;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_id", nullable = false)
    private Opponent away;
    @Column(nullable = false)
    private Instant kickoff;
    @Column(length = 160)
    private String venue;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;
    @Column(nullable = false)
    private int goalsHome;
    @Column(nullable = false)
    private int goalsAway;
}

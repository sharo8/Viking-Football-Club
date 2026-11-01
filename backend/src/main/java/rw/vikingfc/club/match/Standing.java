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
import rw.vikingfc.club.season.Season;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "standings")
public class Standing extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(nullable = false)
    private int positionNo;
    @Column(nullable = false, length = 160)
    private String teamName;
    @Column(nullable = false)
    private int played;
    @Column(nullable = false)
    private int won;
    @Column(nullable = false)
    private int drawn;
    @Column(nullable = false)
    private int lost;
    @Column(nullable = false)
    private int goalsFor;
    @Column(nullable = false)
    private int goalsAgainst;
    @Column(nullable = false)
    private int points;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    private Competition competition;
    @Column(name = "form_text", length = 12)
    private String formText;
    @Column(nullable = false)
    private boolean clubRow;
    @Column(name = "source_kind", nullable = false, length = 20)
    private String sourceKind = "COMPUTED";
}

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
import rw.vikingfc.club.season.Season;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "competitions")
public class Competition extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(name = "source_lang", length = 2)
    private String sourceLang = "en";
    @Column(name = "name_fr", length = 160)
    private String nameFr;
    @Column(name = "name_rw", length = 160)
    private String nameRw;
    @Enumerated(EnumType.STRING)
    @Column(name = "competition_type", nullable = false, length = 20)
    private CompetitionType type = CompetitionType.LEAGUE;
    @Column(length = 400)
    private String logoUrl;
    @Column(nullable = false)
    private boolean official = true;
    @Column(nullable = false)
    private boolean countsForTable;
    @Column(nullable = false)
    private int winPoints = 3;
    @Column(nullable = false)
    private int drawPoints = 1;
    @Column(nullable = false)
    private int lossPoints;
    @Column(nullable = false, length = 16)
    private String color = "#a3283a";
    @Column(nullable = false)
    private int squadSize = 18;
    @Column(nullable = false)
    private int maxBench = 9;
    @Column(nullable = false)
    private int minGoalkeepers = 2;
    @Column(nullable = false)
    private boolean archived;
    @Column(nullable = false, length = 80)
    private String tieBreakers = "POINTS,GD,GF";
}

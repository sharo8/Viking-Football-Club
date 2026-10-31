package rw.vikingfc.club.settings;

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
import rw.vikingfc.club.season.Season;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "club_settings")
public class ClubSettings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false, unique = true)
    private Season season;

    @Column(nullable = false, length = 120)
    private String clubName;

    @Column(nullable = false, length = 200)
    private String motto;

    @Column(nullable = false, length = 200)
    private String competitionName;

    @Column(nullable = false, length = 8)
    private String currencyCode;

    @Column(nullable = false)
    private int playerContractMonths;

    @Column(nullable = false)
    private int playerFirstPhaseMonths;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal playerSocialFund;

    @Column(nullable = false)
    private int staffContractMonths;

    @Column(nullable = false)
    private int staffFirstPhaseMonths;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal staffSocialFund;

    @Column(nullable = false)
    private int attendanceOnTrackPct;

    @Column(nullable = false)
    private int attendanceWatchPct;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal trainingLateFine;

    @Column(nullable = false)
    private int trainingLateMinutes;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal trainingAbsenceFine;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal matchLateFine;

    @Column(nullable = false)
    private int matchLateMinutes;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal matchAbsenceFine;

    @Column(name = "rule6_text", nullable = false, length = 800)
    private String rule6Text;

    @Column(name = "rule7_text", nullable = false, length = 800)
    private String rule7Text;

    @Column(nullable = false)
    private LocalDate firstTrainingMonday;

    @Column(nullable = false)
    private int maxSubstitutes;

    @Column(nullable = false)
    private int matchLengthMinutes;

    @Column(nullable = false)
    private int lineupDeadlineHours = 48;

    @Column(nullable = false)
    private int lineupMaxBench = 9;

    @Column(nullable = false)
    private int lineupMinSquad = 16;

    @Column(nullable = false)
    private int lineupMaxSquad = 23;

    @Column(nullable = false)
    private int ratingCoachPct = 70;

    @Column(nullable = false, precision = 3, scale = 1)
    private java.math.BigDecimal ratingBase = new java.math.BigDecimal("6.0");

    @Column(nullable = false, precision = 3, scale = 1)
    private java.math.BigDecimal ratingGoal = new java.math.BigDecimal("1.0");

    @Column(nullable = false, precision = 3, scale = 1)
    private java.math.BigDecimal ratingAssist = new java.math.BigDecimal("0.7");

    @Column(nullable = false, precision = 3, scale = 1)
    private java.math.BigDecimal ratingClean = new java.math.BigDecimal("0.5");

    @Column(nullable = false, precision = 3, scale = 1)
    private java.math.BigDecimal ratingMotm = new java.math.BigDecimal("0.5");

    @Column(nullable = false, precision = 3, scale = 1)
    private java.math.BigDecimal ratingYellow = new java.math.BigDecimal("0.3");

    @Column(nullable = false, precision = 3, scale = 1)
    private java.math.BigDecimal ratingRed = new java.math.BigDecimal("2.0");

    @Column(nullable = false)
    private boolean publicLineup = true;

    @Column(nullable = false)
    private int yellowSuspension = 5;
}

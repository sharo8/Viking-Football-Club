package rw.vikingfc.club.lineup;

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
import rw.vikingfc.club.match.FootballMatch;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lineup_versions")
public class LineupVersion extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private FootballMatch match;
    @Column(nullable = false)
    private int revision;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LineupState status = LineupState.DRAFT;
    @Column(length = 20)
    private String formation;
    private Integer squadSize;
    @Column(length = 800)
    private String squadPlanJson;
    private Instant publishedAt;
    @Column(length = 120)
    private String publishedBy;
    private Instant deadlineAt;
    private Instant submittedAt;
    @Column(length = 120)
    private String submittedBy;
    private Instant approvedAt;
    @Column(length = 120)
    private String approvedBy;
    private Instant lockedAt;
    @Column(length = 800)
    private String reviewComment;
    @Column(length = 400)
    private String overrideReason;
    @Column(length = 4000)
    private String snapshotJson;
    @Column(length = 120)
    private String editingBy;
    private Instant editingUntil;
    @Column(length = 20)
    private String opponentFormation;
    @Column(length = 2000)
    private String opponentNames;
    private Instant meetingAt;
    @Column(length = 200)
    private String meetingPlace;
    @Column(length = 200)
    private String kitNote;
    @Column(length = 400)
    private String travelNote;
    @Column(length = 2000)
    private String callupMessage;
    @Column(nullable = false)
    private boolean ratingsClosed;
    private Long gkSuggestionId;
}

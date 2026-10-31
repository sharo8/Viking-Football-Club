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
import rw.vikingfc.club.player.Player;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "match_ratings")
public class MatchRating extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private FootballMatch match;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Column(precision = 3, scale = 1)
    private BigDecimal coachRating;
    @Column(precision = 3, scale = 1)
    private BigDecimal systemScore;
    @Column(precision = 3, scale = 1)
    private BigDecimal finalRating;
    @Column(nullable = false)
    private int minutesPlayed;
    @Column(length = 400)
    private String comment;
    @Column(length = 120)
    private String ratedBy;
    @Column(nullable = false)
    private boolean motm;
}

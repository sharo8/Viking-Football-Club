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
import rw.vikingfc.club.player.Player;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "match_player_records")
public class MatchPlayerRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private FootballMatch match;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Participation participation;
    @Column(nullable = false)
    private int minutesPlayed;
    @Column(precision = 4, scale = 1)
    private BigDecimal rating;
    @Column(nullable = false)
    private int goals;
    @Column(nullable = false)
    private int assists;
    @Column(nullable = false)
    private int yellowCards;
    @Column(nullable = false)
    private int redCards;
    @Column(nullable = false)
    private boolean lateToMatch;
    @Column(nullable = false)
    private int minutesLate;
    @Column(nullable = false)
    private boolean participated;
}

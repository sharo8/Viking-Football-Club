package rw.vikingfc.club.contract;

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
import rw.vikingfc.club.season.Season;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "team_captains")
public class TeamCaptain extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Enumerated(EnumType.STRING)
    @Column(name = "captain_rank", nullable = false, length = 20)
    private CaptainRank rank;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(length = 240)
    private String note;
    @Column(name = "assigned_by", length = 160)
    private String assignedBy;
}

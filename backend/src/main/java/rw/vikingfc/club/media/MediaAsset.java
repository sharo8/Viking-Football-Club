package rw.vikingfc.club.media;

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
import rw.vikingfc.club.user.User;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media_assets")
public class MediaAsset extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 400)
    private String url;
    @Column(length = 400)
    private String thumbnailUrl;
    @Column(length = 400)
    private String tags;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private FootballMatch match;
    @Column(length = 1000)
    private String caption;
}

package rw.vikingfc.club.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {
    @Column(nullable = false, length = 24)
    private String kind;
    @Column(length = 160)
    private String title;
    @Column(name = "player_id")
    private Long playerId;
}

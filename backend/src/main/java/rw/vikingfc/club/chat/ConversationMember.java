package rw.vikingfc.club.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.vikingfc.club.common.entity.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conversation_members")
public class ConversationMember extends BaseEntity {
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private boolean pinned;
    @Column(nullable = false)
    private boolean muted;
    @Column(name = "last_read_at")
    private Instant lastReadAt;
}

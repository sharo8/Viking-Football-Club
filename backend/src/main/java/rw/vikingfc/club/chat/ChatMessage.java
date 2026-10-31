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
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(nullable = false, length = 4000)
    private String body;
    @Column(name = "reply_to_id")
    private Long replyToId;
    @Column(nullable = false, length = 16)
    private String status = "SENT";
}

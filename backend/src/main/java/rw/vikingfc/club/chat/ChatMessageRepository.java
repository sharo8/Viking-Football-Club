package rw.vikingfc.club.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtDesc(Long conversationId);
    long countByConversationIdAndCreatedAtAfter(Long conversationId, Instant after);
}

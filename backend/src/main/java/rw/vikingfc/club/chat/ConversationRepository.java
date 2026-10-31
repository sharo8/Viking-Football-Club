package rw.vikingfc.club.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByTitleAndKind(String title, String kind);
    Optional<Conversation> findByPlayerId(Long playerId);
}

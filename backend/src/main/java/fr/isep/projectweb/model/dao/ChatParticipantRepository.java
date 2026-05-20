package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    Optional<ChatParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ChatParticipant> findByConversationIdOrderByJoinedAtAsc(UUID conversationId);
}

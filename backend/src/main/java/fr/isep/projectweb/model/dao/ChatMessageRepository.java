package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID>, JpaSpecificationExecutor<ChatMessage> {

    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(UUID conversationId,
                                                              UUID senderId,
                                                              LocalDateTime createdAt);

    long countByConversationIdAndSenderIdNot(UUID conversationId, UUID senderId);
}

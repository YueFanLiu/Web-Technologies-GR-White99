package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.deletedAt IS NULL
              AND (:before IS NULL OR m.createdAt < :before)
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<ChatMessage> findPageByConversationId(@Param("conversationId") UUID conversationId,
                                               @Param("before") LocalDateTime before,
                                               Pageable pageable);

    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(UUID conversationId,
                                                              UUID senderId,
                                                              LocalDateTime createdAt);

    long countByConversationIdAndSenderIdNot(UUID conversationId, UUID senderId);
}

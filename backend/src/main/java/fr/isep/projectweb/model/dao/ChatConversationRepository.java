package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    @Query("""
            SELECT c
            FROM ChatConversation c
            WHERE c.type = 'DIRECT'
              AND c.event IS NULL
              AND ((c.directUserOne.id = :firstUserId AND c.directUserTwo.id = :secondUserId)
                   OR (c.directUserOne.id = :secondUserId AND c.directUserTwo.id = :firstUserId))
            """)
    Optional<ChatConversation> findDirectConversation(@Param("firstUserId") UUID firstUserId,
                                                      @Param("secondUserId") UUID secondUserId);

    @Query(value = """
            SELECT *
            FROM chat_conversations c
            WHERE c.type = 'DIRECT'
              AND c.event_id IS NULL
              AND LEAST(c.direct_user_one_id, c.direct_user_two_id) = LEAST(CAST(:firstUserId AS uuid), CAST(:secondUserId AS uuid))
              AND GREATEST(c.direct_user_one_id, c.direct_user_two_id) = GREATEST(CAST(:firstUserId AS uuid), CAST(:secondUserId AS uuid))
            LIMIT 1
            """, nativeQuery = true)
    Optional<ChatConversation> findDirectConversationByCanonicalPair(@Param("firstUserId") UUID firstUserId,
                                                                     @Param("secondUserId") UUID secondUserId);

    @Query("""
            SELECT c
            FROM ChatConversation c
            WHERE c.type = 'DIRECT'
              AND c.event.id = :eventId
              AND ((c.directUserOne.id = :firstUserId AND c.directUserTwo.id = :secondUserId)
                   OR (c.directUserOne.id = :secondUserId AND c.directUserTwo.id = :firstUserId))
            """)
    Optional<ChatConversation> findEventDirectConversation(@Param("eventId") UUID eventId,
                                                           @Param("firstUserId") UUID firstUserId,
                                                           @Param("secondUserId") UUID secondUserId);

    @Query("""
            SELECT DISTINCT c
            FROM ChatConversation c
            JOIN ChatParticipant cp ON cp.conversation = c
            WHERE cp.user.id = :userId
            ORDER BY c.updatedAt DESC, c.createdAt DESC
            """)
    List<ChatConversation> findByParticipantUserId(@Param("userId") UUID userId);
}

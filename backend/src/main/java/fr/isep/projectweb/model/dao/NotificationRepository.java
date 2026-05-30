package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    boolean existsByRecipientIdAndDedupeKey(UUID recipientId, String dedupeKey);

    Optional<Notification> findByIdAndRecipientIdAndArchivedAtIsNull(UUID id, UUID recipientId);

    long countByRecipientIdAndReadAtIsNullAndArchivedAtIsNull(UUID recipientId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.recipient.id = :recipientId
              AND n.archivedAt IS NULL
              AND n.readAt IS NULL
            """)
    int markAllAsRead(@Param("recipientId") UUID recipientId,
                      @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.recipient.id = :recipientId
              AND n.archivedAt IS NULL
              AND n.readAt IS NULL
              AND n.type = :type
            """)
    int markAllAsReadByType(@Param("recipientId") UUID recipientId,
                            @Param("type") String type,
                            @Param("readAt") LocalDateTime readAt);
}

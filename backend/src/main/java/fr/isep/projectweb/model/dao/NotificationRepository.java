package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByRecipientIdAndDedupeKey(UUID recipientId, String dedupeKey);

    Optional<Notification> findByIdAndRecipientIdAndArchivedAtIsNull(UUID id, UUID recipientId);

    long countByRecipientIdAndReadAtIsNullAndArchivedAtIsNull(UUID recipientId);

    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.recipient.id = :recipientId
              AND n.archivedAt IS NULL
              AND (:type IS NULL OR n.type = :type)
              AND (:before IS NULL OR n.createdAt < :before)
              AND (:readFilter = 'ALL'
                   OR (:readFilter = 'UNREAD' AND n.readAt IS NULL)
                   OR (:readFilter = 'READ' AND n.readAt IS NOT NULL))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findPageForRecipient(@Param("recipientId") UUID recipientId,
                                            @Param("type") String type,
                                            @Param("before") LocalDateTime before,
                                            @Param("readFilter") String readFilter,
                                            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.recipient.id = :recipientId
              AND n.archivedAt IS NULL
              AND n.readAt IS NULL
              AND (:type IS NULL OR n.type = :type)
            """)
    int markAllAsRead(@Param("recipientId") UUID recipientId,
                      @Param("type") String type,
                      @Param("readAt") LocalDateTime readAt);
}

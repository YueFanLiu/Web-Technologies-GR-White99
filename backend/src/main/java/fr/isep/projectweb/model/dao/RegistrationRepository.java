package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    List<Registration> findByEventIdOrderByRegisteredAtDesc(UUID eventId);

    List<Registration> findByUserIdOrderByRegisteredAtDesc(UUID userId);

    boolean existsByEventIdAndUserIdAndStatusIgnoreCase(UUID eventId, UUID userId, String status);

    List<Registration> findByEventIdAndUserIdOrderByRegisteredAtDesc(UUID eventId, UUID userId);

    @Query("""
            SELECT COUNT(r)
            FROM Registration r
            WHERE r.event.id = :eventId
              AND UPPER(COALESCE(r.status, '')) NOT IN ('CANCELLED', 'CANCELED', 'REJECTED')
            """)
    long countActiveByEventId(@Param("eventId") UUID eventId);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM Registration r
            WHERE r.event.id = :eventId
              AND (:excludeRegistrationId IS NULL OR r.id <> :excludeRegistrationId)
              AND UPPER(COALESCE(r.status, '')) NOT IN ('CANCELLED', 'CANCELED', 'REJECTED')
            """)
    long sumActiveQuantityByEventId(@Param("eventId") UUID eventId,
                                    @Param("excludeRegistrationId") UUID excludeRegistrationId);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM Registration r
            WHERE r.ticketTier.id = :ticketTierId
              AND (:excludeRegistrationId IS NULL OR r.id <> :excludeRegistrationId)
              AND UPPER(COALESCE(r.status, '')) NOT IN ('CANCELLED', 'CANCELED', 'REJECTED')
            """)
    long sumActiveQuantityByTicketTierId(@Param("ticketTierId") UUID ticketTierId,
                                         @Param("excludeRegistrationId") UUID excludeRegistrationId);

    @Query("""
            SELECT r.event.id
            FROM Registration r
            WHERE r.user.id = :userId
              AND UPPER(COALESCE(r.status, '')) NOT IN ('CANCELLED', 'CANCELED', 'REJECTED')
            """)
    List<UUID> findActiveEventIdsByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT r
            FROM Registration r
            JOIN FETCH r.event e
            JOIN FETCH r.user u
            WHERE e.startTime >= :from
              AND e.startTime < :to
              AND UPPER(COALESCE(r.status, '')) NOT IN ('CANCELLED', 'REJECTED')
              AND UPPER(COALESCE(e.status, '')) <> 'CANCELLED'
            """)
    List<Registration> findReminderTargets(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}

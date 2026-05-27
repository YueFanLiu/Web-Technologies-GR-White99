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

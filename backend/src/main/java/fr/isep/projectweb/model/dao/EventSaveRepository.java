package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.EventSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSaveRepository extends JpaRepository<EventSave, UUID> {

    Optional<EventSave> findFirstByUserIdAndEventId(UUID userId, UUID eventId);

    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

    long countByEventId(UUID eventId);

    List<EventSave> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT es.event.id FROM EventSave es WHERE es.user.id = :userId")
    List<UUID> findSavedEventIdsByUserId(@Param("userId") UUID userId);

    long deleteByUserIdAndEventId(UUID userId, UUID eventId);
}

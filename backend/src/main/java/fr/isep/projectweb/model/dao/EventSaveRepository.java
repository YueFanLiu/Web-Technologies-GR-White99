package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.EventSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSaveRepository extends JpaRepository<EventSave, UUID> {

    Optional<EventSave> findFirstByUserIdAndEventId(UUID userId, UUID eventId);

    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

    List<EventSave> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long deleteByUserIdAndEventId(UUID userId, UUID eventId);
}

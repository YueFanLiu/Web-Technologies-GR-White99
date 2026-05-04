package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventImageRepository extends JpaRepository<EventImage, UUID> {

    List<EventImage> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    long countByEventId(UUID eventId);

    Optional<EventImage> findFirstByEventIdOrderByCreatedAtAsc(UUID eventId);

    Optional<EventImage> findByIdAndEventId(UUID id, UUID eventId);
}

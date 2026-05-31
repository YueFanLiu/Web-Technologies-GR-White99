package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.EventReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventReviewRepository extends JpaRepository<EventReview, UUID> {

    List<EventReview> findByEventIdOrderByCreatedAtDesc(UUID eventId);

    Optional<EventReview> findByIdAndEventId(UUID id, UUID eventId);

    long countByEventId(UUID eventId);

    @Query("SELECT AVG(r.rating) FROM EventReview r WHERE r.event.id = :eventId AND r.parent IS NULL")
    Double averageRatingByEventId(@Param("eventId") UUID eventId);
}

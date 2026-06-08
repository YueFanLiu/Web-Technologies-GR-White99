package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.EventTicketTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventTicketTierRepository extends JpaRepository<EventTicketTier, UUID> {

    List<EventTicketTier> findByEventIdOrderBySortOrderAscNameAsc(UUID eventId);

    List<EventTicketTier> findByEventIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID eventId);

    Optional<EventTicketTier> findFirstByEventIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID eventId);

    Optional<EventTicketTier> findByIdAndEventId(UUID id, UUID eventId);
}

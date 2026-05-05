package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.LocationImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationImageRepository extends JpaRepository<LocationImage, UUID> {

    List<LocationImage> findByLocationIdOrderByCreatedAtAsc(UUID locationId);

    long countByLocationId(UUID locationId);

    Optional<LocationImage> findByIdAndLocationId(UUID id, UUID locationId);
}

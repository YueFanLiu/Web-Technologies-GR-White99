package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.AccessibilityPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccessibilityPreferenceRepository extends JpaRepository<AccessibilityPreference, UUID> {

    List<AccessibilityPreference> findByUserId(UUID userId);

    List<AccessibilityPreference> findByLocationId(UUID locationId);

    boolean existsByLocationId(UUID locationId);

    void deleteByUserId(UUID userId);

    void deleteByLocationId(UUID locationId);
}

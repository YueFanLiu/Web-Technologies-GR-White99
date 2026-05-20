package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.UserAccessibilityPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccessibilityPreferenceRepository extends JpaRepository<UserAccessibilityPreference, UUID> {

    Optional<UserAccessibilityPreference> findByUserId(UUID userId);
}

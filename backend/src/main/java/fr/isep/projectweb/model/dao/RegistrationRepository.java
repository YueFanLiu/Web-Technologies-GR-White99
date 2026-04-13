package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
}

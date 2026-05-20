package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
            SELECT u
            FROM User u
            WHERE (:keyword IS NULL
                    OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            ORDER BY u.fullName ASC, u.id ASC
            """)
    List<User> searchPublicUsers(@Param("keyword") String keyword, Pageable pageable);
}

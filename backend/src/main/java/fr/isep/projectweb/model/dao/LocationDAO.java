package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.Location;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LocationDAO extends JpaRepository<Location, UUID> {

    @Query("""
            SELECT l
            FROM Location l
            WHERE LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.city, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.country, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY l.name ASC
            """)
    List<Location> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

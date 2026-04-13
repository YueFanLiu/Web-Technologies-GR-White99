package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("""
            SELECT e
            FROM Event e
            WHERE LOWER(COALESCE(e.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(e.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY e.startTime ASC
            """)
    List<Event> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

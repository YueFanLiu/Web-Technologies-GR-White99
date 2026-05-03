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

    List<Event> findByOrganizerIdOrderByStartTimeAsc(UUID organizerId);

    List<Event> findByLocationIdOrderByStartTimeAsc(UUID locationId);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:keyword IS NULL
                    OR LOWER(COALESCE(e.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(COALESCE(e.category, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (:category IS NULL OR LOWER(e.category) = LOWER(CAST(:category AS string)))
              AND (:status IS NULL OR LOWER(e.status) = LOWER(CAST(:status AS string)))
              AND (:locationId IS NULL OR e.location.id = :locationId)
              AND (:upcomingOnly = false OR e.endTime >= CURRENT_TIMESTAMP)
            ORDER BY e.startTime ASC
            """)
    List<Event> findForMainPage(@Param("keyword") String keyword,
                                @Param("category") String category,
                                @Param("status") String status,
                                @Param("locationId") UUID locationId,
                                @Param("upcomingOnly") boolean upcomingOnly,
                                Pageable pageable);

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

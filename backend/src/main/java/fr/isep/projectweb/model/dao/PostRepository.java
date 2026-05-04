package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Post> findByLocationIdOrderByCreatedAtDesc(UUID locationId);

    List<Post> findByEventIdOrderByCreatedAtDesc(UUID eventId);

    long countByLocationId(UUID locationId);

    @Query("""
            SELECT p
            FROM Post p
            WHERE (:keyword IS NULL
                    OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(COALESCE(p.content, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(COALESCE(p.location.name, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(COALESCE(p.location.city, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(COALESCE(p.event.title, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(COALESCE(p.event.category, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (:status IS NULL OR LOWER(p.status) = LOWER(CAST(:status AS string)))
              AND (:locationId IS NULL OR p.location.id = :locationId)
              AND (:eventId IS NULL OR p.event.id = :eventId)
            ORDER BY p.createdAt DESC
            """)
    List<Post> findForMainFeed(@Param("keyword") String keyword,
                               @Param("status") String status,
                               @Param("locationId") UUID locationId,
                               @Param("eventId") UUID eventId,
                               Pageable pageable);

    @Query("""
            SELECT p
            FROM Post p
            WHERE LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(p.content, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.createdAt DESC
            """)
    List<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.PostReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostReviewRepository extends JpaRepository<PostReview, UUID> {

    List<PostReview> findByPostIdOrderByCreatedAtDesc(UUID postId);

    long countByPostId(UUID postId);

    @Query("SELECT AVG(r.rating) FROM PostReview r WHERE r.post.id = :postId")
    Double averageRatingByPostId(@Param("postId") UUID postId);

    Optional<PostReview> findByIdAndPostId(UUID id, UUID postId);
}

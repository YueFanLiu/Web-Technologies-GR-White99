package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

    List<PostImage> findByPostIdOrderByCreatedAtAsc(UUID postId);

    long countByPostId(UUID postId);

    Optional<PostImage> findByIdAndPostId(UUID id, UUID postId);
}

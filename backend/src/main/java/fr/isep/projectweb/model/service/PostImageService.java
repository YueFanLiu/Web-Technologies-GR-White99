package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.PostImageRepository;
import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dto.request.ImageRequest;
import fr.isep.projectweb.model.dto.response.ImageResponse;
import fr.isep.projectweb.model.entity.Post;
import fr.isep.projectweb.model.entity.PostImage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PostImageService {

    private final PostImageRepository postImageRepository;
    private final PostRepository postRepository;

    public PostImageService(PostImageRepository postImageRepository, PostRepository postRepository) {
        this.postImageRepository = postImageRepository;
        this.postRepository = postRepository;
    }

    public List<ImageResponse> getByPostId(UUID postId) {
        findPost(postId);
        return postImageRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(ResponseMapper::toPostImageResponse)
                .toList();
    }

    public ImageResponse create(UUID postId, ImageRequest request) {
        validateImageUrl(request.getImageUrl());

        PostImage image = new PostImage();
        image.setPost(findPost(postId));
        image.setImageUrl(request.getImageUrl().trim());
        return ResponseMapper.toPostImageResponse(postImageRepository.save(image));
    }

    public void delete(UUID postId, UUID imageId) {
        PostImage image = postImageRepository.findByIdAndPostId(imageId, postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post image not found"));
        postImageRepository.delete(image);
    }

    private Post findPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL must not be blank");
        }
    }
}

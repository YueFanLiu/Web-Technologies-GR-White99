package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.ImageRequest;
import fr.isep.projectweb.model.dto.response.ImageResponse;
import fr.isep.projectweb.model.service.PostImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/images")
@Tag(name = "Post Images", description = "Endpoints for post images")
public class PostImageController {

    private final PostImageService postImageService;

    public PostImageController(PostImageService postImageService) {
        this.postImageService = postImageService;
    }

    @GetMapping
    @Operation(summary = "Get all images for a post")
    public List<ImageResponse> getPostImages(@PathVariable UUID postId) {
        return postImageService.getByPostId(postId);
    }

    @PostMapping
    @Operation(summary = "Add an image to a post")
    public ImageResponse createPostImage(@PathVariable UUID postId, @RequestBody ImageRequest request) {
        return postImageService.create(postId, request);
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete an image from a post")
    public ResponseEntity<Void> deletePostImage(@PathVariable UUID postId, @PathVariable UUID imageId) {
        postImageService.delete(postId, imageId);
        return ResponseEntity.noContent().build();
    }
}

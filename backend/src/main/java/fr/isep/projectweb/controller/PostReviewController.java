package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.ReviewRequest;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.service.PostReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/reviews")
@Tag(name = "Post Reviews", description = "Endpoints for post reviews")
public class PostReviewController {

    private final PostReviewService postReviewService;

    public PostReviewController(PostReviewService postReviewService) {
        this.postReviewService = postReviewService;
    }

    @GetMapping
    @Operation(summary = "Get all reviews for a post")
    public List<ReviewResponse> getPostReviews(@PathVariable UUID postId) {
        return postReviewService.getByPostId(postId);
    }

    @PostMapping
    @Operation(summary = "Add a review to a post")
    public ReviewResponse createPostReview(@PathVariable UUID postId,
                                           @RequestBody ReviewRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        return postReviewService.create(postId, request, jwt);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update a review for a post")
    public ReviewResponse updatePostReview(@PathVariable UUID postId,
                                           @PathVariable UUID reviewId,
                                           @RequestBody ReviewRequest request) {
        return postReviewService.update(postId, reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review from a post")
    public ResponseEntity<Void> deletePostReview(@PathVariable UUID postId, @PathVariable UUID reviewId) {
        postReviewService.delete(postId, reviewId);
        return ResponseEntity.noContent().build();
    }
}

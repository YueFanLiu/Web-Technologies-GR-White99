package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dao.PostReviewRepository;
import fr.isep.projectweb.model.dto.request.ReviewRequest;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.entity.Post;
import fr.isep.projectweb.model.entity.PostReview;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PostReviewService {

    private final PostReviewRepository postReviewRepository;
    private final PostRepository postRepository;
    private final CurrentUserService currentUserService;

    public PostReviewService(PostReviewRepository postReviewRepository,
                             PostRepository postRepository,
                             CurrentUserService currentUserService) {
        this.postReviewRepository = postReviewRepository;
        this.postRepository = postRepository;
        this.currentUserService = currentUserService;
    }

    public List<ReviewResponse> getByPostId(UUID postId) {
        findPost(postId);
        return postReviewRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(ResponseMapper::toPostReviewResponse)
                .toList();
    }

    public ReviewResponse create(UUID postId, ReviewRequest request, Jwt jwt) {
        PostReview review = new PostReview();
        review.setPost(findPost(postId));
        review.setUser(currentUserService.getOrCreateCurrentUser(jwt));
        applyRequest(review, request);
        return ResponseMapper.toPostReviewResponse(postReviewRepository.save(review));
    }

    public ReviewResponse update(UUID postId, UUID reviewId, ReviewRequest request) {
        PostReview review = findReview(postId, reviewId);
        applyRequest(review, request);
        return ResponseMapper.toPostReviewResponse(postReviewRepository.save(review));
    }

    public void delete(UUID postId, UUID reviewId) {
        PostReview review = findReview(postId, reviewId);
        postReviewRepository.delete(review);
    }

    private PostReview findReview(UUID postId, UUID reviewId) {
        return postReviewRepository.findByIdAndPostId(reviewId, postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post review not found"));
    }

    private Post findPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private void applyRequest(PostReview review, ReviewRequest request) {
        validateReview(request);
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
    }

    private void validateReview(ReviewRequest request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        if (request.getComment() == null || request.getComment().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must not be blank");
        }
    }
}

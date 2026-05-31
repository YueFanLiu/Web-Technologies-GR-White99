package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dao.PostReviewRepository;
import fr.isep.projectweb.model.dto.request.ReviewRequest;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Post;
import fr.isep.projectweb.model.entity.PostReview;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PostReviewService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final PostReviewRepository postReviewRepository;
    private final PostRepository postRepository;
    private final CurrentUserService currentUserService;
    private final RecommendationScoreService recommendationScoreService;
    private final NotificationService notificationService;

    public PostReviewService(PostReviewRepository postReviewRepository,
                             PostRepository postRepository,
                             CurrentUserService currentUserService,
                             RecommendationScoreService recommendationScoreService,
                             NotificationService notificationService) {
        this.postReviewRepository = postReviewRepository;
        this.postRepository = postRepository;
        this.currentUserService = currentUserService;
        this.recommendationScoreService = recommendationScoreService;
        this.notificationService = notificationService;
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
        PostReview savedReview = postReviewRepository.save(review);
        recommendationScoreService.recomputePostScore(postId);
        Post post = savedReview.getPost();
        notificationService.create(
                post.getUser(),
                savedReview.getUser(),
                NotificationService.POST_REVIEW_CREATED,
                "New post review",
                savedReview.getUser().getFullName() + " reviewed your post",
                "POST",
                post.getId(),
                "POST_REVIEW",
                savedReview.getId(),
                "post_review:" + savedReview.getId() + ":created",
                java.util.Map.of("postId", post.getId().toString(), "reviewId", savedReview.getId().toString())
        );
        return ResponseMapper.toPostReviewResponse(savedReview);
    }

    public ReviewResponse update(UUID postId, UUID reviewId, ReviewRequest request, Jwt jwt) {
        PostReview review = findReview(postId, reviewId);
        ensureCanManageReview(review, currentUserService.getCurrentUser(jwt));
        applyRequest(review, request);
        PostReview savedReview = postReviewRepository.save(review);
        recommendationScoreService.recomputePostScore(postId);
        return ResponseMapper.toPostReviewResponse(savedReview);
    }

    public void delete(UUID postId, UUID reviewId, Jwt jwt) {
        PostReview review = findReview(postId, reviewId);
        ensureCanManageReview(review, currentUserService.getCurrentUser(jwt));
        postReviewRepository.delete(review);
        recommendationScoreService.recomputePostScore(postId);
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
        review.setParent(resolveParentReview(review.getPost().getId(), request.getParentId()));
    }

    private PostReview resolveParentReview(UUID postId, UUID parentId) {
        if (parentId == null) {
            return null;
        }

        PostReview parent = findReview(postId, parentId);
        if (parent.getParent() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replies can only target top-level comments");
        }
        return parent;
    }

    private void validateReview(ReviewRequest request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        if (request.getComment() == null || request.getComment().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must not be blank");
        }
    }

    private void ensureCanManageReview(PostReview review, User currentUser) {
        if (isAdmin(currentUser)
                || isReviewAuthor(review, currentUser)
                || isPostAuthor(review.getPost(), currentUser)
                || isRelatedEventOrganizer(review.getPost(), currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own post reviews");
    }

    private boolean isReviewAuthor(PostReview review, User user) {
        return review.getUser() != null && user != null && Objects.equals(review.getUser().getId(), user.getId());
    }

    private boolean isPostAuthor(Post post, User user) {
        return post != null
                && post.getUser() != null
                && user != null
                && Objects.equals(post.getUser().getId(), user.getId());
    }

    private boolean isRelatedEventOrganizer(Post post, User user) {
        Event event = post != null ? post.getEvent() : null;
        return isOrganizer(user)
                && event != null
                && event.getOrganizer() != null
                && Objects.equals(event.getOrganizer().getId(), user.getId());
    }

    private boolean isOrganizer(User user) {
        return user != null && ORGANIZER_ROLE.equalsIgnoreCase(user.getRole());
    }

    private boolean isAdmin(User user) {
        return user != null && ADMIN_ROLE.equalsIgnoreCase(user.getRole());
    }
}

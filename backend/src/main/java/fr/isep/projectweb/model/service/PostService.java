package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dto.request.PostRequest;
import fr.isep.projectweb.model.dto.response.PostResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Post;
import fr.isep.projectweb.model.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PostService {

    private static final int SEARCH_RESULT_LIMIT = 20;
    private static final int DEFAULT_MAIN_FEED_LIMIT = 20;
    private static final int MAX_MAIN_FEED_LIMIT = 100;
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final PostRepository postRepository;
    private final LocationDAO locationDAO;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final RecommendationScoreService recommendationScoreService;

    public PostService(PostRepository postRepository,
                       LocationDAO locationDAO,
                       EventRepository eventRepository,
                       CurrentUserService currentUserService,
                       RecommendationScoreService recommendationScoreService) {
        this.postRepository = postRepository;
        this.locationDAO = locationDAO;
        this.eventRepository = eventRepository;
        this.currentUserService = currentUserService;
        this.recommendationScoreService = recommendationScoreService;
    }

    public PostResponse createPost(PostRequest request, Jwt jwt) {
        Post post = new Post();
        post.setUser(currentUserService.getOrCreateCurrentUser(jwt));
        applyRequest(post, request);
        Post savedPost = postRepository.save(post);
        recommendationScoreService.recomputePostScore(savedPost.getId());
        refreshLocation(savedPost.getLocation());
        return ResponseMapper.toPostResponse(savedPost);
    }

    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> getMainFeedPosts(String keyword,
                                               String status,
                                               UUID locationId,
                                               UUID eventId,
                                               Integer limit) {
        int resultLimit = normalizeLimit(limit);
        String normalizedKeyword = normalizeOptional(keyword);

        return postRepository.findForMainFeed(
                        normalizedKeyword,
                        normalizeOptional(status),
                        locationId,
                        eventId,
                        PageRequest.of(0, resultLimit)
                )
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> getPostsByUserId(UUID userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> getPostsByLocationId(UUID locationId) {
        return postRepository.findByLocationIdOrderByCreatedAtDesc(locationId)
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> getPostsByEventId(UUID eventId) {
        return postRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> searchPosts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keyword must not be blank");
        }

        return postRepository.searchByKeyword(keyword.trim(), PageRequest.of(0, SEARCH_RESULT_LIMIT))
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public PostResponse getPostById(UUID id) {
        return ResponseMapper.toPostResponse(findPostById(id));
    }

    public PostResponse updatePost(UUID id, PostRequest request, Jwt jwt) {
        Post post = findPostById(id);
        ensureCanManagePost(post, currentUserService.getCurrentUser(jwt));
        UUID previousLocationId = post.getLocation() != null ? post.getLocation().getId() : null;
        applyRequest(post, request);
        Post savedPost = postRepository.save(post);
        recommendationScoreService.recomputePostScore(savedPost.getId());
        refreshChangedLocations(previousLocationId, savedPost.getLocation() != null ? savedPost.getLocation().getId() : null);
        return ResponseMapper.toPostResponse(savedPost);
    }

    public void deletePost(UUID id, Jwt jwt) {
        Post post = findPostById(id);
        ensureCanManagePost(post, currentUserService.getCurrentUser(jwt));
        UUID locationId = post.getLocation() != null ? post.getLocation().getId() : null;
        postRepository.delete(post);
        if (locationId != null) {
            recommendationScoreService.recomputeLocationScore(locationId);
        }
    }

    private void applyRequest(Post post, PostRequest request) {
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setStatus(request.getStatus());
        post.setLocation(findLocation(request.getLocationId()));
        post.setEvent(findEvent(request.getEventId()));
    }

    private Post findPostById(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private Location findLocation(UUID locationId) {
        if (locationId == null) {
            return null;
        }

        return locationDAO.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    private Event findEvent(UUID eventId) {
        if (eventId == null) {
            return null;
        }

        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_MAIN_FEED_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_MAIN_FEED_LIMIT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void refreshLocation(Location location) {
        if (location != null) {
            recommendationScoreService.recomputeLocationScore(location.getId());
        }
    }

    private void refreshChangedLocations(UUID previousLocationId, UUID currentLocationId) {
        if (previousLocationId != null) {
            recommendationScoreService.recomputeLocationScore(previousLocationId);
        }
        if (currentLocationId != null && !Objects.equals(previousLocationId, currentLocationId)) {
            recommendationScoreService.recomputeLocationScore(currentLocationId);
        }
    }

    private void ensureCanManagePost(Post post, User currentUser) {
        if (isAdmin(currentUser) || isPostAuthor(post, currentUser) || isRelatedEventOrganizer(post, currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own posts");
    }

    private boolean isPostAuthor(Post post, User user) {
        return post.getUser() != null && user != null && Objects.equals(post.getUser().getId(), user.getId());
    }

    private boolean isRelatedEventOrganizer(Post post, User user) {
        Event event = post.getEvent();
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

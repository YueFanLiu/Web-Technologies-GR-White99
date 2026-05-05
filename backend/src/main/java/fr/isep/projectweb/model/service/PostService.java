package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.algorithm.recommendation.post.PostRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.post.PostRecommendationScorer;
import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dao.PostImageRepository;
import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dao.PostReviewRepository;
import fr.isep.projectweb.model.dto.request.PostRequest;
import fr.isep.projectweb.model.dto.response.PostResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Post;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PostService {

    private static final int SEARCH_RESULT_LIMIT = 20;
    private static final int DEFAULT_MAIN_FEED_LIMIT = 20;
    private static final int MAX_MAIN_FEED_LIMIT = 100;
    private static final int MAIN_FEED_CANDIDATE_LIMIT = 100;

    private final PostRepository postRepository;
    private final LocationDAO locationDAO;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final PostImageRepository postImageRepository;
    private final PostReviewRepository postReviewRepository;
    private final PostRecommendationScorer postRecommendationScorer;

    public PostService(PostRepository postRepository,
                       LocationDAO locationDAO,
                       EventRepository eventRepository,
                       CurrentUserService currentUserService,
                       PostImageRepository postImageRepository,
                       PostReviewRepository postReviewRepository,
                       PostRecommendationScorer postRecommendationScorer) {
        this.postRepository = postRepository;
        this.locationDAO = locationDAO;
        this.eventRepository = eventRepository;
        this.currentUserService = currentUserService;
        this.postImageRepository = postImageRepository;
        this.postReviewRepository = postReviewRepository;
        this.postRecommendationScorer = postRecommendationScorer;
    }

    public PostResponse createPost(PostRequest request, Jwt jwt) {
        Post post = new Post();
        post.setUser(currentUserService.getOrCreateCurrentUser(jwt));
        applyRequest(post, request);
        return ResponseMapper.toPostResponse(postRepository.save(post));
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
        LocalDateTime now = LocalDateTime.now();

        return postRepository.findForMainFeed(
                        normalizedKeyword,
                        normalizeOptional(status),
                        locationId,
                        eventId,
                        PageRequest.of(0, MAIN_FEED_CANDIDATE_LIMIT)
                )
                .stream()
                .map(post -> new ScoredPost(
                        post,
                        postRecommendationScorer.score(toRecommendationFeatures(post, normalizedKeyword, now))
                ))
                .sorted(Comparator
                        .comparingDouble(ScoredPost::score)
                        .reversed()
                        .thenComparing(scored -> scored.post().getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(scored -> normalizeSortText(scored.post().getTitle()))
                        .thenComparing(scored -> scored.post().getId()))
                .limit(resultLimit)
                .map(scored -> ResponseMapper.toPostResponse(scored.post()))
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

    public PostResponse updatePost(UUID id, PostRequest request) {
        Post post = findPostById(id);
        applyRequest(post, request);
        return ResponseMapper.toPostResponse(postRepository.save(post));
    }

    public void deletePost(UUID id) {
        Post post = findPostById(id);
        postRepository.delete(post);
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

    private PostRecommendationFeatures toRecommendationFeatures(Post post,
                                                                String keyword,
                                                                LocalDateTime now) {
        PostRecommendationFeatures features = new PostRecommendationFeatures();
        UUID postId = post.getId();
        Location location = post.getLocation();
        Event event = post.getEvent();

        features.setKeyword(keyword);
        features.setTitle(post.getTitle());
        features.setContent(post.getContent());
        features.setStatus(post.getStatus());
        features.setNow(now);
        features.setCreatedAt(post.getCreatedAt());

        features.setHasLocation(location != null);
        if (location != null) {
            features.setLocationName(location.getName());
            features.setLocationCity(location.getCity());
        }

        features.setHasEvent(event != null);
        if (event != null) {
            features.setEventTitle(event.getTitle());
            features.setEventCategory(event.getCategory());
            features.setEventStartTime(event.getStartTime());
            features.setEventEndTime(event.getEndTime());
        }

        features.setAverageRating(postReviewRepository.averageRatingByPostId(postId));
        features.setReviewCount(postReviewRepository.countByPostId(postId));
        features.setImageCount(postImageRepository.countByPostId(postId));

        return features;
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

    private String normalizeSortText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredPost(Post post, double score) {
    }
}

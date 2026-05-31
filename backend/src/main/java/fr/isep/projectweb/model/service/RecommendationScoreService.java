package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.algorithm.recommendation.event.EventRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.event.EventRecommendationScorer;
import fr.isep.projectweb.model.algorithm.recommendation.location.LocationRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.location.LocationRecommendationScorer;
import fr.isep.projectweb.model.algorithm.recommendation.post.PostRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.post.PostRecommendationScorer;
import fr.isep.projectweb.model.dao.EventImageRepository;
import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.EventSaveRepository;
import fr.isep.projectweb.model.dao.AccessibilityPreferenceRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dao.LocationImageRepository;
import fr.isep.projectweb.model.dao.PostImageRepository;
import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dao.PostReviewRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.entity.AccessibilityPreference;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Post;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecommendationScoreService {

    private final EventRepository eventRepository;
    private final PostRepository postRepository;
    private final LocationDAO locationDAO;
    private final EventImageRepository eventImageRepository;
    private final EventReviewRepository eventReviewRepository;
    private final EventSaveRepository eventSaveRepository;
    private final RegistrationRepository registrationRepository;
    private final PostImageRepository postImageRepository;
    private final PostReviewRepository postReviewRepository;
    private final LocationImageRepository locationImageRepository;
    private final AccessibilityPreferenceRepository accessibilityPreferenceRepository;
    private final EventRecommendationScorer eventRecommendationScorer;
    private final PostRecommendationScorer postRecommendationScorer;
    private final LocationRecommendationScorer locationRecommendationScorer;

    public RecommendationScoreService(EventRepository eventRepository,
                                      PostRepository postRepository,
                                      LocationDAO locationDAO,
                                      EventImageRepository eventImageRepository,
                                      EventReviewRepository eventReviewRepository,
                                      EventSaveRepository eventSaveRepository,
                                      RegistrationRepository registrationRepository,
                                      PostImageRepository postImageRepository,
                                      PostReviewRepository postReviewRepository,
                                      LocationImageRepository locationImageRepository,
                                      AccessibilityPreferenceRepository accessibilityPreferenceRepository,
                                      EventRecommendationScorer eventRecommendationScorer,
                                      PostRecommendationScorer postRecommendationScorer,
                                      LocationRecommendationScorer locationRecommendationScorer) {
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
        this.locationDAO = locationDAO;
        this.eventImageRepository = eventImageRepository;
        this.eventReviewRepository = eventReviewRepository;
        this.eventSaveRepository = eventSaveRepository;
        this.registrationRepository = registrationRepository;
        this.postImageRepository = postImageRepository;
        this.postReviewRepository = postReviewRepository;
        this.locationImageRepository = locationImageRepository;
        this.accessibilityPreferenceRepository = accessibilityPreferenceRepository;
        this.eventRecommendationScorer = eventRecommendationScorer;
        this.postRecommendationScorer = postRecommendationScorer;
        this.locationRecommendationScorer = locationRecommendationScorer;
    }

    @Transactional
    public void recomputeEventScore(UUID eventId) {
        eventRepository.findById(eventId).ifPresent(event -> {
            LocalDateTime now = LocalDateTime.now();
            event.setRecommendationScore(eventRecommendationScorer.score(toEventFeatures(event, now)));
            event.setRecommendationScoreUpdatedAt(now);
            eventRepository.save(event);
        });
    }

    @Transactional
    public void recomputePostScore(UUID postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setRecommendationScore(postRecommendationScorer.score(toPostFeatures(post, LocalDateTime.now())));
            post.setRecommendationScoreUpdatedAt(LocalDateTime.now());
            postRepository.save(post);
        });
    }

    @Transactional
    public void recomputeLocationScore(UUID locationId) {
        locationDAO.findById(locationId).ifPresent(location -> {
            location.setRecommendationScore(locationRecommendationScorer.score(toLocationFeatures(location)));
            location.setRecommendationScoreUpdatedAt(LocalDateTime.now());
            locationDAO.save(location);
        });
    }

    @Transactional
    public void recomputePostScoresByEvent(UUID eventId) {
        postRepository.findByEventId(eventId)
                .forEach(post -> recomputePostScore(post.getId()));
    }

    @Transactional
    public void recomputeAllScores() {
        eventRepository.findAll().forEach(event -> recomputeEventScore(event.getId()));
        postRepository.findAll().forEach(post -> recomputePostScore(post.getId()));
        locationDAO.findAll().forEach(location -> recomputeLocationScore(location.getId()));
    }

    @Transactional
    public void recomputeUnscoredScores() {
        eventRepository.findByRecommendationScoreUpdatedAtIsNull()
                .forEach(event -> recomputeEventScore(event.getId()));
        postRepository.findByRecommendationScoreUpdatedAtIsNull()
                .forEach(post -> recomputePostScore(post.getId()));
        locationDAO.findByRecommendationScoreUpdatedAtIsNull()
                .forEach(location -> recomputeLocationScore(location.getId()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recomputeScoresOnStartup() {
        recomputeUnscoredScores();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void recomputeTimeSensitiveScores() {
        recomputeAllScores();
    }

    private EventRecommendationFeatures toEventFeatures(Event event, LocalDateTime now) {
        EventRecommendationFeatures features = new EventRecommendationFeatures();
        Location location = event.getLocation();
        UUID eventId = event.getId();

        features.setKeyword(null);
        features.setTitle(event.getTitle());
        features.setDescription(event.getDescription());
        features.setCategory(event.getCategory());
        features.setHasLocation(location != null);
        if (location != null) {
            features.setLocationName(location.getName());
            features.setLocationCity(location.getCity());
        }
        features.setNow(now);
        features.setStartTime(event.getStartTime());
        features.setEndTime(event.getEndTime());
        features.setStatus(event.getStatus());
        features.setCapacity(event.getCapacity());
        features.setPrice(event.getPrice());
        features.setVirtualEvent(event.getIsVirtual());
        features.setAverageRating(eventReviewRepository.averageRatingByEventId(eventId));
        features.setReviewCount(eventReviewRepository.countByEventId(eventId));
        features.setImageCount(eventImageRepository.countByEventId(eventId));
        features.setActiveRegistrationCount(registrationRepository.countActiveByEventId(eventId));
        features.setFavoriteCount(eventSaveRepository.countByEventId(eventId));

        return features;
    }

    private PostRecommendationFeatures toPostFeatures(Post post, LocalDateTime now) {
        PostRecommendationFeatures features = new PostRecommendationFeatures();
        UUID postId = post.getId();
        Location location = post.getLocation();
        Event event = post.getEvent();

        features.setKeyword(null);
        features.setTitle(post.getTitle());
        features.setContent(post.getContent());
        features.setStatus(post.getStatus());
        features.setNow(now);
        features.setCreatedAt(post.getCreatedAt() != null ? post.getCreatedAt() : now);
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

    private LocationRecommendationFeatures toLocationFeatures(Location location) {
        LocationRecommendationFeatures features = new LocationRecommendationFeatures();
        UUID locationId = location.getId();

        features.setKeyword(null);
        features.setName(location.getName());
        features.setDescription(location.getDescription());
        features.setAddress(location.getAddress());
        features.setCity(location.getCity());
        features.setCountry(location.getCountry());
        features.setHasCoordinates(location.getLatitude() != null && location.getLongitude() != null);
        features.setEventCount(toIntCount(eventRepository.countByLocationId(locationId)));
        features.setUpcomingEventCount(toIntCount(eventRepository.countUpcomingByLocationId(locationId)));
        features.setPostCount(toIntCount(postRepository.countByLocationId(locationId)));
        features.setImageCount(toIntCount(locationImageRepository.countByLocationId(locationId)));

        applyAccessibilityFeatures(features, accessibilityPreferenceRepository.findByLocationId(locationId));

        return features;
    }

    private void applyAccessibilityFeatures(LocationRecommendationFeatures features,
                                            Iterable<AccessibilityPreference> preferences) {
        Set<String> featureKeys = java.util.stream.StreamSupport.stream(preferences.spliterator(), false)
                .map(AccessibilityPreference::getFeatureKey)
                .collect(Collectors.toSet());

        features.setWheelchairAccessible(featureKeys.contains(AccessibilityFeatureKeys.WHEELCHAIR_ACCESSIBLE));
        features.setHasElevator(featureKeys.contains(AccessibilityFeatureKeys.ELEVATOR));
        features.setAccessibleToilet(featureKeys.contains(AccessibilityFeatureKeys.ACCESSIBLE_RESTROOM));
        features.setQuietEnvironment(featureKeys.contains(AccessibilityFeatureKeys.QUIET_ENVIRONMENT));
        features.setStepFreeAccess(featureKeys.contains(AccessibilityFeatureKeys.STEP_FREE_ACCESS));
    }

    private int toIntCount(long count) {
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }
}

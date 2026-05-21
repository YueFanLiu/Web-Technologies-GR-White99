package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dto.request.ReviewRequest;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventReview;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class EventReviewService {

    private final EventReviewRepository eventReviewRepository;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final RecommendationScoreService recommendationScoreService;
    private final NotificationService notificationService;

    public EventReviewService(EventReviewRepository eventReviewRepository,
                              EventRepository eventRepository,
                              CurrentUserService currentUserService,
                              RecommendationScoreService recommendationScoreService,
                              NotificationService notificationService) {
        this.eventReviewRepository = eventReviewRepository;
        this.eventRepository = eventRepository;
        this.currentUserService = currentUserService;
        this.recommendationScoreService = recommendationScoreService;
        this.notificationService = notificationService;
    }

    public List<ReviewResponse> getByEventId(UUID eventId) {
        findEvent(eventId);
        return eventReviewRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(ResponseMapper::toEventReviewResponse)
                .toList();
    }

    public ReviewResponse create(UUID eventId, ReviewRequest request, Jwt jwt) {
        EventReview review = new EventReview();
        review.setEvent(findEvent(eventId));
        review.setUser(currentUserService.getOrCreateCurrentUser(jwt));
        applyRequest(review, request);
        EventReview savedReview = eventReviewRepository.save(review);
        recommendationScoreService.recomputeEventScore(eventId);
        Event event = savedReview.getEvent();
        notificationService.create(
                event.getOrganizer(),
                savedReview.getUser(),
                NotificationService.EVENT_REVIEW_CREATED,
                "New event review",
                savedReview.getUser().getFullName() + " reviewed your event",
                "EVENT",
                event.getId(),
                "EVENT_REVIEW",
                savedReview.getId(),
                "event_review:" + savedReview.getId() + ":created",
                java.util.Map.of("eventId", event.getId().toString(), "reviewId", savedReview.getId().toString())
        );
        return ResponseMapper.toEventReviewResponse(savedReview);
    }

    public ReviewResponse update(UUID eventId, UUID reviewId, ReviewRequest request) {
        EventReview review = findReview(eventId, reviewId);
        applyRequest(review, request);
        EventReview savedReview = eventReviewRepository.save(review);
        recommendationScoreService.recomputeEventScore(eventId);
        return ResponseMapper.toEventReviewResponse(savedReview);
    }

    public void delete(UUID eventId, UUID reviewId) {
        EventReview review = findReview(eventId, reviewId);
        eventReviewRepository.delete(review);
        recommendationScoreService.recomputeEventScore(eventId);
    }

    private EventReview findReview(UUID eventId, UUID reviewId) {
        return eventReviewRepository.findByIdAndEventId(reviewId, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event review not found"));
    }

    private Event findEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void applyRequest(EventReview review, ReviewRequest request) {
        validateReview(request);
        review.setRating(request.getRating());
        review.setComment(normalizeComment(request.getComment()));
    }

    private void validateReview(ReviewRequest request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        return comment.trim();
    }
}

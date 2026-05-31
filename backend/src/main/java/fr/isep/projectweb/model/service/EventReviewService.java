package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.ReviewRequest;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventReview;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventReviewService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";
    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private final EventReviewRepository eventReviewRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final CurrentUserService currentUserService;
    private final RecommendationScoreService recommendationScoreService;
    private final NotificationService notificationService;

    public EventReviewService(EventReviewRepository eventReviewRepository,
                              EventRepository eventRepository,
                              RegistrationRepository registrationRepository,
                              CurrentUserService currentUserService,
                              RecommendationScoreService recommendationScoreService,
                              NotificationService notificationService) {
        this.eventReviewRepository = eventReviewRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
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
        Event event = findEvent(eventId);
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        ensureCanCreateReview(event, currentUser);

        EventReview review = new EventReview();
        review.setEvent(event);
        review.setUser(currentUser);
        applyRequest(review, request);
        EventReview savedReview = eventReviewRepository.save(review);
        recommendationScoreService.recomputeEventScore(eventId);
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

    public ReviewResponse update(UUID eventId, UUID reviewId, ReviewRequest request, Jwt jwt) {
        EventReview review = findReview(eventId, reviewId);
        ensureCanManageReview(review, currentUserService.getCurrentUser(jwt));
        applyRequest(review, request);
        EventReview savedReview = eventReviewRepository.save(review);
        recommendationScoreService.recomputeEventScore(eventId);
        return ResponseMapper.toEventReviewResponse(savedReview);
    }

    public void delete(UUID eventId, UUID reviewId, Jwt jwt) {
        EventReview review = findReview(eventId, reviewId);
        ensureCanManageReview(review, currentUserService.getCurrentUser(jwt));
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

    private void ensureCanCreateReview(Event event, User currentUser) {
        if (event.getStartTime() == null || event.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review an event after it has started");
        }

        if (!registrationRepository.existsByEventIdAndUserIdAndStatusIgnoreCase(
                event.getId(),
                currentUser.getId(),
                CONFIRMED_STATUS
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review events you registered for");
        }
    }

    private void ensureCanManageReview(EventReview review, User currentUser) {
        if (isAdmin(currentUser) || isReviewAuthor(review, currentUser) || isEventOrganizer(review.getEvent(), currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own event reviews");
    }

    private boolean isReviewAuthor(EventReview review, User user) {
        return review.getUser() != null && user != null && Objects.equals(review.getUser().getId(), user.getId());
    }

    private boolean isEventOrganizer(Event event, User user) {
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

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        return comment.trim();
    }
}

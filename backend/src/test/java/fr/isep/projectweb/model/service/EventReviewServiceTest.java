package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.ReviewRequest;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventReview;
import fr.isep.projectweb.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventReviewServiceTest {

    @Mock
    private EventReviewRepository eventReviewRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private RecommendationScoreService recommendationScoreService;
    @Mock
    private NotificationService notificationService;

    private EventReviewService eventReviewService;
    private User attendee;
    private User organizer;
    private Event startedEvent;

    @BeforeEach
    void setUp() {
        eventReviewService = new EventReviewService(
                eventReviewRepository,
                eventRepository,
                registrationRepository,
                currentUserService,
                recommendationScoreService,
                notificationService
        );

        attendee = user("attendee@example.com", "Attendee", "PARENT");
        organizer = user("organizer@example.com", "Organizer", "ORGANIZER");

        startedEvent = new Event();
        startedEvent.setId(UUID.randomUUID());
        startedEvent.setTitle("Started Event");
        startedEvent.setOrganizer(organizer);
        startedEvent.setStartTime(LocalDateTime.now().minusHours(1));
        startedEvent.setEndTime(LocalDateTime.now().plusHours(1));
        startedEvent.setCapacity(20);
    }

    @Test
    void rejectsReviewWhenUserHasNoConfirmedRegistration() {
        ReviewRequest request = reviewRequest(5, "Great event", null);
        when(eventRepository.findById(startedEvent.getId())).thenReturn(Optional.of(startedEvent));
        when(currentUserService.getOrCreateCurrentUser(null)).thenReturn(attendee);
        when(registrationRepository.existsByEventIdAndUserIdAndStatusIgnoreCase(startedEvent.getId(), attendee.getId(), "CONFIRMED"))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> eventReviewService.create(startedEvent.getId(), request, null)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(eventReviewRepository, never()).save(any(EventReview.class));
    }

    @Test
    void createsReviewForConfirmedRegistration() {
        ReviewRequest request = reviewRequest(5, "  Very accessible  ", null);
        mockCanCreateReview();
        when(eventReviewRepository.save(any(EventReview.class))).thenAnswer(invocation -> {
            EventReview review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });

        ReviewResponse response = eventReviewService.create(startedEvent.getId(), request, null);

        assertEquals(5, response.getRating());
        assertEquals("Very accessible", response.getComment());
        verify(recommendationScoreService).recomputeEventScore(startedEvent.getId());
        verify(notificationService).create(
                eq(organizer),
                eq(attendee),
                eq(NotificationService.EVENT_REVIEW_CREATED),
                anyString(),
                anyString(),
                anyString(),
                eq(startedEvent.getId()),
                anyString(),
                any(UUID.class),
                anyString(),
                anyMap()
        );
    }

    @Test
    void allowsReplyToTopLevelReviewWithoutRating() {
        EventReview parent = new EventReview();
        parent.setId(UUID.randomUUID());
        parent.setEvent(startedEvent);
        parent.setUser(attendee);
        parent.setRating(5);
        parent.setComment("Top level review");

        ReviewRequest request = reviewRequest(null, "Reply text", parent.getId());
        mockCanCreateReview();
        when(eventReviewRepository.findByIdAndEventId(parent.getId(), startedEvent.getId())).thenReturn(Optional.of(parent));
        when(eventReviewRepository.save(any(EventReview.class))).thenAnswer(invocation -> {
            EventReview reply = invocation.getArgument(0);
            reply.setId(UUID.randomUUID());
            return reply;
        });

        ReviewResponse response = eventReviewService.create(startedEvent.getId(), request, null);

        assertEquals("Reply text", response.getComment());
        assertEquals(parent.getId(), response.getParentId());
    }

    private void mockCanCreateReview() {
        when(eventRepository.findById(startedEvent.getId())).thenReturn(Optional.of(startedEvent));
        when(currentUserService.getOrCreateCurrentUser(null)).thenReturn(attendee);
        when(registrationRepository.existsByEventIdAndUserIdAndStatusIgnoreCase(startedEvent.getId(), attendee.getId(), "CONFIRMED"))
                .thenReturn(true);
    }

    private ReviewRequest reviewRequest(Integer rating, String comment, UUID parentId) {
        ReviewRequest request = new ReviewRequest();
        request.setRating(rating);
        request.setComment(comment);
        request.setParentId(parentId);
        return request;
    }

    private User user(String email, String fullName, String role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }
}

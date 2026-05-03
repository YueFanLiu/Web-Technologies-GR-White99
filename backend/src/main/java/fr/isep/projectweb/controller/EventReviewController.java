package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.ReviewRequest;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.service.EventReviewService;
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
@RequestMapping("/api/events/{eventId}/reviews")
@Tag(name = "Event Reviews", description = "Endpoints for event reviews")
public class EventReviewController {

    private final EventReviewService eventReviewService;

    public EventReviewController(EventReviewService eventReviewService) {
        this.eventReviewService = eventReviewService;
    }

    @GetMapping
    @Operation(summary = "Get all reviews for an event")
    public List<ReviewResponse> getEventReviews(@PathVariable UUID eventId) {
        return eventReviewService.getByEventId(eventId);
    }

    @PostMapping
    @Operation(summary = "Add a review to an event")
    public ReviewResponse createEventReview(@PathVariable UUID eventId,
                                            @RequestBody ReviewRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        return eventReviewService.create(eventId, request, jwt);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update a review for an event")
    public ReviewResponse updateEventReview(@PathVariable UUID eventId,
                                            @PathVariable UUID reviewId,
                                            @RequestBody ReviewRequest request) {
        return eventReviewService.update(eventId, reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review from an event")
    public ResponseEntity<Void> deleteEventReview(@PathVariable UUID eventId, @PathVariable UUID reviewId) {
        eventReviewService.delete(eventId, reviewId);
        return ResponseEntity.noContent().build();
    }
}

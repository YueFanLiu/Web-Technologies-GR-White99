package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.EventRequest;
import fr.isep.projectweb.model.dto.response.EventAnalyticsResponse;
import fr.isep.projectweb.model.dto.response.EventResponse;
import fr.isep.projectweb.model.service.EventAnalyticsService;
import fr.isep.projectweb.model.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "CRUD endpoints for events")
public class EventController {

    private final EventService eventService;
    private final EventAnalyticsService eventAnalyticsService;

    public EventController(EventService eventService, EventAnalyticsService eventAnalyticsService) {
        this.eventService = eventService;
        this.eventAnalyticsService = eventAnalyticsService;
    }

    @PostMapping
    @Operation(summary = "Create an event")
    public EventResponse createEvent(@RequestBody EventRequest request, @AuthenticationPrincipal Jwt jwt) {
        return eventService.createEvent(request, jwt);
    }

    @GetMapping
    @Operation(summary = "Get events for the main activity page")
    public List<EventResponse> getEvents(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) UUID locationId,
                                         @RequestParam(required = false) Boolean upcomingOnly,
                                         @RequestParam(required = false) Integer limit) {
        return eventService.getMainPageEvents(keyword, category, status, locationId, upcomingOnly, limit);
    }

    @GetMapping("/organizer/{organizerId}")
    @Operation(summary = "Get events by organizer id")
    public List<EventResponse> getEventsByOrganizerId(@PathVariable UUID organizerId) {
        return eventService.getEventsByOrganizerId(organizerId);
    }

    @GetMapping("/location/{locationId}")
    @Operation(summary = "Get events by location id")
    public List<EventResponse> getEventsByLocationId(@PathVariable UUID locationId) {
        return eventService.getEventsByLocationId(locationId);
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter events")
    public List<EventResponse> searchEvents(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String locationId,
                                            @RequestParam(name = "location", required = false) String location,
                                            @RequestParam(required = false) String date,
                                            @RequestParam(required = false) List<String> activityType,
                                            @RequestParam(required = false) List<String> accessibilityOptions) {
        return eventService.searchEvents(keyword, locationId != null ? locationId : location, date, activityType, accessibilityOptions);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular events based on registrations, favorites, reviews, ratings, and timing")
    public List<EventResponse> getPopularEvents(@RequestParam(required = false) Integer limit) {
        return eventService.getPopularEvents(limit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one event by id")
    public EventResponse getEventById(@PathVariable UUID id) {
        return eventService.getEventById(id);
    }

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Get event analytics")
    public EventAnalyticsResponse getEventAnalytics(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return eventAnalyticsService.getEventAnalytics(id, jwt);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an event")
    public EventResponse updateEvent(@PathVariable UUID id,
                                     @RequestBody EventRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return eventService.updateEvent(id, request, jwt);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        eventService.deleteEvent(id, jwt);
        return ResponseEntity.noContent().build();
    }
}

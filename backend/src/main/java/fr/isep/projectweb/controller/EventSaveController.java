package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.response.EventSaveResponse;
import fr.isep.projectweb.model.dto.response.EventSaveStatusResponse;
import fr.isep.projectweb.model.service.EventSaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Event Saves", description = "Current user saved event endpoints")
public class EventSaveController {

    private final EventSaveService eventSaveService;

    public EventSaveController(EventSaveService eventSaveService) {
        this.eventSaveService = eventSaveService;
    }

    @PostMapping("/events/{eventId}/save")
    @Operation(summary = "Save an event for the current authenticated user")
    public EventSaveResponse saveEvent(@PathVariable UUID eventId, @AuthenticationPrincipal Jwt jwt) {
        return eventSaveService.saveEvent(eventId, jwt);
    }

    @GetMapping("/events/{eventId}/save")
    @Operation(summary = "Get whether the current authenticated user saved an event")
    public EventSaveStatusResponse getSaveStatus(@PathVariable UUID eventId, @AuthenticationPrincipal Jwt jwt) {
        return eventSaveService.getSaveStatus(eventId, jwt);
    }

    @DeleteMapping("/events/{eventId}/save")
    @Operation(summary = "Remove a saved event for the current authenticated user")
    public ResponseEntity<Void> unsaveEvent(@PathVariable UUID eventId, @AuthenticationPrincipal Jwt jwt) {
        eventSaveService.unsaveEvent(eventId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/saved-events")
    @Operation(summary = "Get the current authenticated user's saved events")
    public List<EventSaveResponse> getMySavedEvents(@AuthenticationPrincipal Jwt jwt) {
        return eventSaveService.getMySavedEvents(jwt);
    }
}

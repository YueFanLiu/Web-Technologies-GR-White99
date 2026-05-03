package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.ImageRequest;
import fr.isep.projectweb.model.dto.response.ImageResponse;
import fr.isep.projectweb.model.service.EventImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/images")
@Tag(name = "Event Images", description = "Endpoints for event images")
public class EventImageController {

    private final EventImageService eventImageService;

    public EventImageController(EventImageService eventImageService) {
        this.eventImageService = eventImageService;
    }

    @GetMapping
    @Operation(summary = "Get all images for an event")
    public List<ImageResponse> getEventImages(@PathVariable UUID eventId) {
        return eventImageService.getByEventId(eventId);
    }

    @PostMapping
    @Operation(summary = "Add an image to an event")
    public ImageResponse createEventImage(@PathVariable UUID eventId, @RequestBody ImageRequest request) {
        return eventImageService.create(eventId, request);
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete an image from an event")
    public ResponseEntity<Void> deleteEventImage(@PathVariable UUID eventId, @PathVariable UUID imageId) {
        eventImageService.delete(eventId, imageId);
        return ResponseEntity.noContent().build();
    }
}

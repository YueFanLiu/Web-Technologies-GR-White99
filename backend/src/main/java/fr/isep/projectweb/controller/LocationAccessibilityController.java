package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.LocationAccessibilityRequest;
import fr.isep.projectweb.model.dto.response.LocationAccessibilityResponse;
import fr.isep.projectweb.model.service.LocationAccessibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/locations/{locationId}/accessibility")
@Tag(name = "Location Accessibility", description = "CRUD endpoints for location accessibility")
public class LocationAccessibilityController {

    private final LocationAccessibilityService locationAccessibilityService;

    public LocationAccessibilityController(LocationAccessibilityService locationAccessibilityService) {
        this.locationAccessibilityService = locationAccessibilityService;
    }

    @GetMapping
    @Operation(summary = "Get accessibility information for a location")
    public LocationAccessibilityResponse getAccessibility(@PathVariable UUID locationId) {
        return locationAccessibilityService.getByLocationId(locationId);
    }

    @PostMapping
    @Operation(summary = "Create accessibility information for a location")
    public LocationAccessibilityResponse createAccessibility(@PathVariable UUID locationId,
                                                             @RequestBody LocationAccessibilityRequest request) {
        return locationAccessibilityService.create(locationId, request);
    }

    @PutMapping
    @Operation(summary = "Update accessibility information for a location")
    public LocationAccessibilityResponse updateAccessibility(@PathVariable UUID locationId,
                                                             @RequestBody LocationAccessibilityRequest request) {
        return locationAccessibilityService.update(locationId, request);
    }

    @DeleteMapping
    @Operation(summary = "Delete accessibility information for a location")
    public ResponseEntity<Void> deleteAccessibility(@PathVariable UUID locationId) {
        locationAccessibilityService.delete(locationId);
        return ResponseEntity.noContent().build();
    }
}

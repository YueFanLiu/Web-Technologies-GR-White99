package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.ImageRequest;
import fr.isep.projectweb.model.dto.response.ImageResponse;
import fr.isep.projectweb.model.service.LocationImageService;
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
@RequestMapping("/api/locations/{locationId}/images")
@Tag(name = "Location Images", description = "Endpoints for location images")
public class LocationImageController {

    private final LocationImageService locationImageService;

    public LocationImageController(LocationImageService locationImageService) {
        this.locationImageService = locationImageService;
    }

    @GetMapping
    @Operation(summary = "Get all images for a location")
    public List<ImageResponse> getLocationImages(@PathVariable UUID locationId) {
        return locationImageService.getByLocationId(locationId);
    }

    @PostMapping
    @Operation(summary = "Add an image to a location")
    public ImageResponse createLocationImage(@PathVariable UUID locationId, @RequestBody ImageRequest request) {
        return locationImageService.create(locationId, request);
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete an image from a location")
    public ResponseEntity<Void> deleteLocationImage(@PathVariable UUID locationId, @PathVariable UUID imageId) {
        locationImageService.delete(locationId, imageId);
        return ResponseEntity.noContent().build();
    }
}

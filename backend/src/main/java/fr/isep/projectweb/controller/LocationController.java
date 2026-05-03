package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.LocationRequest;
import fr.isep.projectweb.model.dto.response.LocationResponse;
import fr.isep.projectweb.model.service.LocationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "CRUD endpoints for locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    @Operation(summary = "Create a location")
    public LocationResponse createLocation(@RequestBody LocationRequest request) {
        return locationService.createLocation(request);
    }

    @GetMapping
    @Operation(summary = "Get all locations")
    public List<LocationResponse> getAllLocations() {
        return locationService.getAllLocations();
    }

    @GetMapping("/search")
    @Operation(summary = "Search locations by keyword")
    public List<LocationResponse> searchLocations(@RequestParam String keyword) {
        return locationService.searchLocations(keyword);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one location by id")
    public LocationResponse getLocationById(@PathVariable UUID id) {
        return locationService.getLocationById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a location")
    public LocationResponse updateLocation(@PathVariable UUID id, @RequestBody LocationRequest request) {
        return locationService.updateLocation(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a location")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}

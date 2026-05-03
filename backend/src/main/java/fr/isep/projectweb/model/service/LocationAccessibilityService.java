package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.LocationAccessibilityRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dto.request.LocationAccessibilityRequest;
import fr.isep.projectweb.model.dto.response.LocationAccessibilityResponse;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.LocationAccessibility;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class LocationAccessibilityService {

    private final LocationAccessibilityRepository locationAccessibilityRepository;
    private final LocationDAO locationDAO;

    public LocationAccessibilityService(LocationAccessibilityRepository locationAccessibilityRepository,
                                        LocationDAO locationDAO) {
        this.locationAccessibilityRepository = locationAccessibilityRepository;
        this.locationDAO = locationDAO;
    }

    public LocationAccessibilityResponse getByLocationId(UUID locationId) {
        ensureLocationExists(locationId);
        return locationAccessibilityRepository.findByLocationId(locationId)
                .map(ResponseMapper::toLocationAccessibilityResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location accessibility not found"));
    }

    public LocationAccessibilityResponse create(UUID locationId, LocationAccessibilityRequest request) {
        Location location = findLocation(locationId);

        if (locationAccessibilityRepository.findByLocationId(locationId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location accessibility already exists");
        }

        LocationAccessibility accessibility = new LocationAccessibility();
        accessibility.setLocation(location);
        applyRequest(accessibility, request);
        return ResponseMapper.toLocationAccessibilityResponse(locationAccessibilityRepository.save(accessibility));
    }

    public LocationAccessibilityResponse update(UUID locationId, LocationAccessibilityRequest request) {
        LocationAccessibility accessibility = locationAccessibilityRepository.findByLocationId(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location accessibility not found"));

        applyRequest(accessibility, request);
        return ResponseMapper.toLocationAccessibilityResponse(locationAccessibilityRepository.save(accessibility));
    }

    public void delete(UUID locationId) {
        LocationAccessibility accessibility = locationAccessibilityRepository.findByLocationId(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location accessibility not found"));

        locationAccessibilityRepository.delete(accessibility);
    }

    private void applyRequest(LocationAccessibility accessibility, LocationAccessibilityRequest request) {
        accessibility.setWheelchairAccessible(request.getWheelchairAccessible());
        accessibility.setHasElevator(request.getHasElevator());
        accessibility.setAccessibleToilet(request.getAccessibleToilet());
        accessibility.setQuietEnvironment(request.getQuietEnvironment());
        accessibility.setStepFreeAccess(request.getStepFreeAccess());
        accessibility.setNotes(request.getNotes());
    }

    private void ensureLocationExists(UUID locationId) {
        findLocation(locationId);
    }

    private Location findLocation(UUID locationId) {
        return locationDAO.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }
}

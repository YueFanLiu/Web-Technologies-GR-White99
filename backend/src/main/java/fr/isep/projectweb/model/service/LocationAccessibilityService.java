package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.AccessibilityPreferenceRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dto.request.LocationAccessibilityRequest;
import fr.isep.projectweb.model.dto.response.LocationAccessibilityResponse;
import fr.isep.projectweb.model.entity.AccessibilityPreference;
import fr.isep.projectweb.model.entity.Location;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocationAccessibilityService {

    private final AccessibilityPreferenceRepository accessibilityPreferenceRepository;
    private final LocationDAO locationDAO;
    private final RecommendationScoreService recommendationScoreService;

    public LocationAccessibilityService(AccessibilityPreferenceRepository accessibilityPreferenceRepository,
                                        LocationDAO locationDAO,
                                        RecommendationScoreService recommendationScoreService) {
        this.accessibilityPreferenceRepository = accessibilityPreferenceRepository;
        this.locationDAO = locationDAO;
        this.recommendationScoreService = recommendationScoreService;
    }

    public LocationAccessibilityResponse getByLocationId(UUID locationId) {
        Location location = findLocation(locationId);
        List<AccessibilityPreference> preferences = accessibilityPreferenceRepository.findByLocationId(locationId);
        if (preferences.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location accessibility not found");
        }
        return toResponse(location, preferences);
    }

    @Transactional
    public LocationAccessibilityResponse create(UUID locationId, LocationAccessibilityRequest request) {
        Location location = findLocation(locationId);

        if (accessibilityPreferenceRepository.existsByLocationId(locationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location accessibility already exists");
        }

        List<AccessibilityPreference> newPreferences = toLocationPreferences(location, request);
        ensureAtLeastOneLocationFeature(newPreferences);
        List<AccessibilityPreference> savedPreferences = accessibilityPreferenceRepository.saveAll(newPreferences);
        recommendationScoreService.recomputeLocationScore(locationId);
        return toResponse(location, savedPreferences);
    }

    @Transactional
    public LocationAccessibilityResponse update(UUID locationId, LocationAccessibilityRequest request) {
        Location location = findLocation(locationId);
        if (!accessibilityPreferenceRepository.existsByLocationId(locationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location accessibility not found");
        }

        List<AccessibilityPreference> newPreferences = toLocationPreferences(location, request);
        ensureAtLeastOneLocationFeature(newPreferences);
        accessibilityPreferenceRepository.deleteByLocationId(locationId);
        accessibilityPreferenceRepository.flush();
        List<AccessibilityPreference> savedPreferences = accessibilityPreferenceRepository.saveAll(newPreferences);
        recommendationScoreService.recomputeLocationScore(locationId);
        return toResponse(location, savedPreferences);
    }

    @Transactional
    public void delete(UUID locationId) {
        if (!accessibilityPreferenceRepository.existsByLocationId(locationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location accessibility not found");
        }

        accessibilityPreferenceRepository.deleteByLocationId(locationId);
        recommendationScoreService.recomputeLocationScore(locationId);
    }

    private List<AccessibilityPreference> toLocationPreferences(Location location, LocationAccessibilityRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must not be null");
        }

        return java.util.stream.Stream.of(
                        toLocationPreference(location, AccessibilityFeatureKeys.WHEELCHAIR_ACCESSIBLE,
                                request.getWheelchairAccessible(), request.getNotes()),
                        toLocationPreference(location, AccessibilityFeatureKeys.ELEVATOR,
                                request.getHasElevator(), request.getNotes()),
                        toLocationPreference(location, AccessibilityFeatureKeys.ACCESSIBLE_RESTROOM,
                                request.getAccessibleToilet(), request.getNotes()),
                        toLocationPreference(location, AccessibilityFeatureKeys.QUIET_ENVIRONMENT,
                                request.getQuietEnvironment(), request.getNotes()),
                        toLocationPreference(location, AccessibilityFeatureKeys.STEP_FREE_ACCESS,
                                request.getStepFreeAccess(), request.getNotes())
                )
                .filter(preference -> preference != null)
                .toList();
    }

    private AccessibilityPreference toLocationPreference(Location location, String featureKey, Boolean enabled, String notes) {
        if (!Boolean.TRUE.equals(enabled)) {
            return null;
        }

        AccessibilityPreference preference = new AccessibilityPreference();
        preference.setLocation(location);
        preference.setFeatureKey(featureKey);
        preference.setNotes(notes);
        return preference;
    }

    private void ensureAtLeastOneLocationFeature(List<AccessibilityPreference> preferences) {
        if (preferences.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one accessibility feature must be enabled; use DELETE to remove accessibility information"
            );
        }
    }

    private LocationAccessibilityResponse toResponse(Location location, List<AccessibilityPreference> preferences) {
        Set<String> featureKeys = preferences.stream()
                .map(AccessibilityPreference::getFeatureKey)
                .collect(Collectors.toSet());

        LocationAccessibilityResponse response = new LocationAccessibilityResponse();
        response.setId(preferences.stream().findFirst().map(AccessibilityPreference::getId).orElse(null));
        response.setLocation(ResponseMapper.toLocationResponse(location));
        response.setWheelchairAccessible(featureKeys.contains(AccessibilityFeatureKeys.WHEELCHAIR_ACCESSIBLE));
        response.setHasElevator(featureKeys.contains(AccessibilityFeatureKeys.ELEVATOR));
        response.setAccessibleToilet(featureKeys.contains(AccessibilityFeatureKeys.ACCESSIBLE_RESTROOM));
        response.setQuietEnvironment(featureKeys.contains(AccessibilityFeatureKeys.QUIET_ENVIRONMENT));
        response.setStepFreeAccess(featureKeys.contains(AccessibilityFeatureKeys.STEP_FREE_ACCESS));
        response.setNotes(preferences.stream()
                .map(AccessibilityPreference::getNotes)
                .filter(notes -> notes != null && !notes.isBlank())
                .findFirst()
                .orElse(null));
        response.setCreatedAt(preferences.stream().findFirst().map(AccessibilityPreference::getCreatedAt).orElse(null));
        response.setUpdatedAt(preferences.stream().findFirst().map(AccessibilityPreference::getUpdatedAt).orElse(null));
        return response;
    }

    private Location findLocation(UUID locationId) {
        return locationDAO.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }
}

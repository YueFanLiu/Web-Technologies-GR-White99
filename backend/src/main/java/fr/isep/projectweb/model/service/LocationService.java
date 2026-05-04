package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.algorithm.recommendation.location.LocationRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.location.LocationRecommendationScorer;
import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.LocationAccessibilityRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dao.LocationImageRepository;
import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dto.request.LocationRequest;
import fr.isep.projectweb.model.dto.response.LocationResponse;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.LocationAccessibility;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocationService {

    private static final int SEARCH_RESULT_LIMIT = 20;
    private static final int SEARCH_CANDIDATE_LIMIT = 100;

    private final LocationDAO locationDAO;
    private final EventRepository eventRepository;
    private final PostRepository postRepository;
    private final LocationImageRepository locationImageRepository;
    private final LocationAccessibilityRepository locationAccessibilityRepository;
    private final LocationRecommendationScorer locationRecommendationScorer;

    public LocationService(LocationDAO locationDAO,
                           EventRepository eventRepository,
                           PostRepository postRepository,
                           LocationImageRepository locationImageRepository,
                           LocationAccessibilityRepository locationAccessibilityRepository,
                           LocationRecommendationScorer locationRecommendationScorer) {
        this.locationDAO = locationDAO;
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
        this.locationImageRepository = locationImageRepository;
        this.locationAccessibilityRepository = locationAccessibilityRepository;
        this.locationRecommendationScorer = locationRecommendationScorer;
    }

    public LocationResponse createLocation(LocationRequest request) {
        Location location = new Location();
        location.setName(request.getName());
        location.setDescription(request.getDescription());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setCountry(request.getCountry());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());

        return ResponseMapper.toLocationResponse(locationDAO.save(location));
    }

    public List<LocationResponse> getAllLocations() {
        return locationDAO.findAll()
                .stream()
                .map(ResponseMapper::toLocationResponse)
                .toList();
    }

    public List<LocationResponse> searchLocations(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keyword must not be blank");
        }

        String normalizedKeyword = keyword.trim();
        return locationDAO.searchByKeyword(normalizedKeyword, PageRequest.of(0, SEARCH_CANDIDATE_LIMIT))
                .stream()
                .map(location -> new ScoredLocation(
                        location,
                        locationRecommendationScorer.score(toRecommendationFeatures(location, normalizedKeyword))
                ))
                .sorted(Comparator
                        .comparingDouble(ScoredLocation::score)
                        .reversed()
                        .thenComparing(scored -> normalizeSortText(scored.location().getName()))
                        .thenComparing(scored -> scored.location().getId()))
                .limit(SEARCH_RESULT_LIMIT)
                .map(scored -> ResponseMapper.toLocationResponse(scored.location()))
                .toList();
    }

    public LocationResponse getLocationById(UUID id) {
        return ResponseMapper.toLocationResponse(findLocationById(id));
    }

    public LocationResponse updateLocation(UUID id, LocationRequest request) {
        Location location = findLocationById(id);
        location.setName(request.getName());
        location.setDescription(request.getDescription());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setCountry(request.getCountry());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());

        return ResponseMapper.toLocationResponse(locationDAO.save(location));
    }

    public void deleteLocation(UUID id) {
        Location location = findLocationById(id);
        locationDAO.delete(location);
    }

    private Location findLocationById(UUID id) {
        return locationDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    private LocationRecommendationFeatures toRecommendationFeatures(Location location, String keyword) {
        LocationRecommendationFeatures features = new LocationRecommendationFeatures();
        UUID locationId = location.getId();

        features.setKeyword(keyword);
        features.setName(location.getName());
        features.setDescription(location.getDescription());
        features.setAddress(location.getAddress());
        features.setCity(location.getCity());
        features.setCountry(location.getCountry());
        features.setHasCoordinates(location.getLatitude() != null && location.getLongitude() != null);

        features.setEventCount(toIntCount(eventRepository.countByLocationId(locationId)));
        features.setUpcomingEventCount(toIntCount(eventRepository.countUpcomingByLocationId(locationId)));
        features.setPostCount(toIntCount(postRepository.countByLocationId(locationId)));
        features.setImageCount(toIntCount(locationImageRepository.countByLocationId(locationId)));

        locationAccessibilityRepository.findByLocationId(locationId)
                .ifPresent(accessibility -> applyAccessibilityFeatures(features, accessibility));

        return features;
    }

    private void applyAccessibilityFeatures(LocationRecommendationFeatures features,
                                            LocationAccessibility accessibility) {
        features.setWheelchairAccessible(accessibility.getWheelchairAccessible());
        features.setHasElevator(accessibility.getHasElevator());
        features.setAccessibleToilet(accessibility.getAccessibleToilet());
        features.setQuietEnvironment(accessibility.getQuietEnvironment());
        features.setStepFreeAccess(accessibility.getStepFreeAccess());
    }

    private int toIntCount(long count) {
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private String normalizeSortText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredLocation(Location location, double score) {
    }
}

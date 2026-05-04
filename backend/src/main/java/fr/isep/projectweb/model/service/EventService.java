package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.algorithm.recommendation.event.EventRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.event.EventRecommendationScorer;
import fr.isep.projectweb.model.dao.EventImageRepository;
import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dto.request.EventRequest;
import fr.isep.projectweb.model.dto.response.EventResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventImage;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventService {

    private static final int SEARCH_RESULT_LIMIT = 20;
    private static final int MAIN_PAGE_CANDIDATE_LIMIT = 100;
    private static final int DEFAULT_MAIN_PAGE_LIMIT = 20;
    private static final int MAX_MAIN_PAGE_LIMIT = 100;
    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final EventRepository eventRepository;
    private final EventImageRepository eventImageRepository;
    private final EventReviewRepository eventReviewRepository;
    private final LocationDAO locationDAO;
    private final CurrentUserService currentUserService;
    private final EventRecommendationScorer eventRecommendationScorer;

    public EventService(EventRepository eventRepository,
                        EventImageRepository eventImageRepository,
                        EventReviewRepository eventReviewRepository,
                        LocationDAO locationDAO,
                        CurrentUserService currentUserService,
                        EventRecommendationScorer eventRecommendationScorer) {
        this.eventRepository = eventRepository;
        this.eventImageRepository = eventImageRepository;
        this.eventReviewRepository = eventReviewRepository;
        this.locationDAO = locationDAO;
        this.currentUserService = currentUserService;
        this.eventRecommendationScorer = eventRecommendationScorer;
    }

    public EventResponse createEvent(EventRequest request, Jwt jwt) {
        User organizer = currentUserService.getOrCreateCurrentUser(jwt);
        ensureOrganizer(organizer);

        Event event = new Event();
        event.setOrganizer(organizer);
        applyRequest(event, request);

        return toResponse(eventRepository.save(event), true);
    }

    public List<EventResponse> getMainPageEvents(String keyword,
                                                 String category,
                                                 String status,
                                                 UUID locationId,
                                                 Boolean upcomingOnly,
                                                 Integer limit) {
        int resultLimit = normalizeLimit(limit);
        boolean filterUpcoming = upcomingOnly == null || upcomingOnly;
        String normalizedKeyword = normalizeOptional(keyword);
        LocalDateTime now = LocalDateTime.now();

        return eventRepository.findForMainPage(
                        normalizedKeyword,
                        normalizeOptional(category),
                        normalizeOptional(status),
                        locationId,
                        filterUpcoming,
                        PageRequest.of(0, MAIN_PAGE_CANDIDATE_LIMIT)
                )
                .stream()
                .map(event -> new ScoredEvent(
                        event,
                        eventRecommendationScorer.score(toRecommendationFeatures(event, normalizedKeyword, now))
                ))
                .sorted(Comparator
                        .comparingDouble(ScoredEvent::score)
                        .reversed()
                        .thenComparing(scored -> scored.event().getStartTime(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(scored -> normalizeSortText(scored.event().getTitle()))
                        .thenComparing(scored -> scored.event().getId()))
                .limit(resultLimit)
                .map(scored -> toResponse(scored.event(), false))
                .toList();
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(event -> toResponse(event, false))
                .toList();
    }

    public List<EventResponse> getEventsByOrganizerId(UUID organizerId) {
        return eventRepository.findByOrganizerIdOrderByStartTimeAsc(organizerId)
                .stream()
                .map(event -> toResponse(event, false))
                .toList();
    }

    public List<EventResponse> getEventsByLocationId(UUID locationId) {
        return eventRepository.findByLocationIdOrderByStartTimeAsc(locationId)
                .stream()
                .map(event -> toResponse(event, false))
                .toList();
    }

    public List<EventResponse> searchEvents(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keyword must not be blank");
        }

        return eventRepository.searchByKeyword(keyword.trim(), PageRequest.of(0, SEARCH_RESULT_LIMIT))
                .stream()
                .map(event -> toResponse(event, false))
                .toList();
    }

    public EventResponse getEventById(UUID id) {
        return toResponse(findEventById(id), true);
    }

    public EventResponse updateEvent(UUID id, EventRequest request, Jwt jwt) {
        Event event = findEventById(id);
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        ensureEventOrganizer(event, currentUser);

        applyRequest(event, request);
        return toResponse(eventRepository.save(event), true);
    }

    public void deleteEvent(UUID id, Jwt jwt) {
        Event event = findEventById(id);
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        ensureEventOrganizer(event, currentUser);
        eventRepository.delete(event);
    }

    private Event findEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void applyRequest(Event event, EventRequest request) {
        validateEventRequest(request);
        event.setTitle(request.getTitle().trim());
        event.setDescription(normalizeOptional(request.getDescription()));
        event.setCategory(normalizeOptional(request.getCategory()));
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setCapacity(request.getCapacity());
        event.setPrice(request.getPrice());
        event.setIsVirtual(Boolean.TRUE.equals(request.getIsVirtual()));
        event.setStatus(normalizeStatus(request.getStatus()));
        event.setLocation(findLocation(request.getLocationId()));
    }

    private void validateEventRequest(EventRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must not be blank");
        }
        if (request.getStartTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must not be blank");
        }
        if (request.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must not be blank");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (request.getCapacity() == null || request.getCapacity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacity must be at least 1");
        }
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must not be negative");
        }
        if (request.getLocationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location id must not be blank");
        }
    }

    private Location findLocation(UUID locationId) {
        return locationDAO.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    private EventRecommendationFeatures toRecommendationFeatures(Event event,
                                                                 String keyword,
                                                                 LocalDateTime now) {
        EventRecommendationFeatures features = new EventRecommendationFeatures();
        Location location = event.getLocation();
        UUID eventId = event.getId();

        features.setKeyword(keyword);
        features.setTitle(event.getTitle());
        features.setDescription(event.getDescription());
        features.setCategory(event.getCategory());

        features.setHasLocation(location != null);
        if (location != null) {
            features.setLocationName(location.getName());
            features.setLocationCity(location.getCity());
        }

        features.setNow(now);
        features.setStartTime(event.getStartTime());
        features.setEndTime(event.getEndTime());
        features.setStatus(event.getStatus());
        features.setCapacity(event.getCapacity());
        features.setPrice(event.getPrice());
        features.setVirtualEvent(event.getIsVirtual());

        features.setAverageRating(eventReviewRepository.averageRatingByEventId(eventId));
        features.setReviewCount(eventReviewRepository.countByEventId(eventId));
        features.setImageCount(eventImageRepository.countByEventId(eventId));

        return features;
    }

    private void ensureOrganizer(User user) {
        if (!ORGANIZER_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organizers can manage events");
        }
    }

    private void ensureEventOrganizer(Event event, User currentUser) {
        ensureOrganizer(currentUser);
        if (event.getOrganizer() == null || !Objects.equals(event.getOrganizer().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event organizer can manage this event");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_MAIN_PAGE_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_MAIN_PAGE_LIMIT);
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeOptional(status);
        return normalized != null ? normalized.toUpperCase() : "DRAFT";
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSortText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private EventResponse toResponse(Event event, boolean includeAllImages) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setDescription(event.getDescription());
        response.setCategory(event.getCategory());
        response.setStartTime(event.getStartTime());
        response.setEndTime(event.getEndTime());
        response.setCapacity(event.getCapacity());
        response.setPrice(event.getPrice());
        response.setIsVirtual(event.getIsVirtual());
        response.setStatus(event.getStatus());
        response.setOrganizer(toUserSummary(event.getOrganizer()));
        response.setLocation(toLocationSummary(event.getLocation()));
        response.setCoverImageUrl(resolveCoverImageUrl(event.getId()));
        response.setImageUrls(includeAllImages ? resolveImageUrls(event.getId()) : List.of());
        response.setAverageRating(eventReviewRepository.averageRatingByEventId(event.getId()));
        response.setReviewCount(eventReviewRepository.countByEventId(event.getId()));
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        return response;
    }

    private EventResponse.UserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        EventResponse.UserSummary summary = new EventResponse.UserSummary();
        summary.setId(user.getId());
        summary.setFullName(user.getFullName());
        summary.setRole(user.getRole());
        return summary;
    }

    private EventResponse.LocationSummary toLocationSummary(Location location) {
        if (location == null) {
            return null;
        }
        EventResponse.LocationSummary summary = new EventResponse.LocationSummary();
        summary.setId(location.getId());
        summary.setName(location.getName());
        summary.setAddress(location.getAddress());
        summary.setCity(location.getCity());
        summary.setCountry(location.getCountry());
        summary.setLatitude(location.getLatitude());
        summary.setLongitude(location.getLongitude());
        return summary;
    }

    private String resolveCoverImageUrl(UUID eventId) {
        return eventImageRepository.findFirstByEventIdOrderByCreatedAtAsc(eventId)
                .map(EventImage::getImageUrl)
                .orElse(null);
    }

    private List<String> resolveImageUrls(UUID eventId) {
        return eventImageRepository.findByEventIdOrderByCreatedAtAsc(eventId)
                .stream()
                .map(EventImage::getImageUrl)
                .toList();
    }

    private record ScoredEvent(Event event, double score) {
    }
}

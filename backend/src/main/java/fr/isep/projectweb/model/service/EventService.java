package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventImageRepository;
import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.EventRequest;
import fr.isep.projectweb.model.dto.response.EventResponse;
import fr.isep.projectweb.model.entity.AccessibilityPreference;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventImage;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventService {

    private static final int SEARCH_RESULT_LIMIT = 20;
    private static final int DEFAULT_MAIN_PAGE_LIMIT = 20;
    private static final int MAX_MAIN_PAGE_LIMIT = 1000;
    private static final String ORGANIZER_ROLE = "ORGANIZER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final EventRepository eventRepository;
    private final EventImageRepository eventImageRepository;
    private final EventReviewRepository eventReviewRepository;
    private final LocationDAO locationDAO;
    private final RegistrationRepository registrationRepository;
    private final CurrentUserService currentUserService;
    private final RecommendationScoreService recommendationScoreService;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepository,
                        EventImageRepository eventImageRepository,
                        EventReviewRepository eventReviewRepository,
                        LocationDAO locationDAO,
                        RegistrationRepository registrationRepository,
                        CurrentUserService currentUserService,
                        RecommendationScoreService recommendationScoreService,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.eventImageRepository = eventImageRepository;
        this.eventReviewRepository = eventReviewRepository;
        this.locationDAO = locationDAO;
        this.registrationRepository = registrationRepository;
        this.currentUserService = currentUserService;
        this.recommendationScoreService = recommendationScoreService;
        this.notificationService = notificationService;
    }

    public EventResponse createEvent(EventRequest request, Jwt jwt) {
        User organizer = currentUserService.getOrCreateCurrentUser(jwt);
        ensureOrganizer(organizer);

        Event event = new Event();
        event.setOrganizer(organizer);
        applyRequest(event, request);

        Event savedEvent = eventRepository.save(event);
        recommendationScoreService.recomputeEventScore(savedEvent.getId());
        recommendationScoreService.recomputeLocationScore(savedEvent.getLocation().getId());
        return toResponse(savedEvent, true);
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

        return eventRepository.findForMainPage(
                        normalizedKeyword,
                        normalizeOptional(category),
                        normalizeOptional(status),
                        locationId,
                        filterUpcoming,
                        PageRequest.of(0, resultLimit)
                )
                .stream()
                .map(event -> toResponse(event, false))
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

    public List<EventResponse> getPopularEvents(Integer limit) {
        int resultLimit = Math.min(normalizeLimit(limit), 10);
        return eventRepository.findPopularEvents(PageRequest.of(0, resultLimit))
                .stream()
                .map(event -> toResponse(event, false))
                .toList();
    }

    public List<EventResponse> searchEvents(String keyword,
                                            String locationId,
                                            String date,
                                            List<String> activityTypes,
                                            List<String> accessibilityOptions) {
        String normalizedKeyword = normalizeOptional(keyword);
        UUID normalizedLocationId = normalizeOptionalUuid(locationId, "locationId");
        LocalDate normalizedDate = normalizeOptionalDate(date);
        List<String> normalizedActivityTypes = normalizeOptionalFilters(activityTypes);
        AccessibilityFilter accessibilityFilter = normalizeAccessibilityOptions(accessibilityOptions);

        if (normalizedKeyword == null
                && normalizedLocationId == null
                && normalizedDate == null
                && normalizedActivityTypes.isEmpty()
                && accessibilityFilter.isEmpty()) {
            return getMainPageEvents(null, null, null, null, null, null);
        }

        return eventRepository.findAll(
                        buildSearchSpecification(
                                normalizedKeyword,
                                normalizedLocationId,
                                normalizedDate,
                                normalizedActivityTypes,
                                accessibilityFilter
                        ),
                        PageRequest.of(0, SEARCH_RESULT_LIMIT, Sort.by(
                                Sort.Order.desc("recommendationScore"),
                                Sort.Order.asc("startTime"),
                                Sort.Order.asc("id")
                        ))
                )
                .getContent()
                .stream()
                .map(event -> toResponse(event, false))
                .toList();
    }

    private Specification<Event> buildSearchSpecification(String keyword,
                                                          UUID locationId,
                                                          LocalDate date,
                                                          List<String> activityTypes,
                                                          AccessibilityFilter accessibilityFilter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("title"), "")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("category"), "")), pattern)
                ));
            }

            if (locationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("location").get("id"), locationId));
            }

            if (date != null) {
                LocalDateTime dateStart = date.atStartOfDay();
                LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();
                predicates.add(criteriaBuilder.and(
                        criteriaBuilder.lessThan(root.get("startTime"), dateEnd),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endTime"), dateStart)
                ));
            }

            if (!activityTypes.isEmpty()) {
                predicates.add(criteriaBuilder.lower(root.get("category")).in(
                        activityTypes.stream()
                                .map(activityType -> activityType.toLowerCase(Locale.ROOT))
                                .toList()
                ));
            }

            if (!accessibilityFilter.isEmpty()) {
                if (accessibilityFilter.wheelchairAccessible()) {
                    predicates.add(hasAccessibilityFeature(root, query, criteriaBuilder,
                            AccessibilityFeatureKeys.WHEELCHAIR_ACCESSIBLE));
                }
                if (accessibilityFilter.hasElevator()) {
                    predicates.add(hasAccessibilityFeature(root, query, criteriaBuilder,
                            AccessibilityFeatureKeys.ELEVATOR));
                }
                if (accessibilityFilter.accessibleToilet()) {
                    predicates.add(hasAccessibilityFeature(root, query, criteriaBuilder,
                            AccessibilityFeatureKeys.ACCESSIBLE_RESTROOM));
                }
                if (accessibilityFilter.quietEnvironment()) {
                    predicates.add(hasAccessibilityFeature(root, query, criteriaBuilder,
                            AccessibilityFeatureKeys.QUIET_ENVIRONMENT));
                }
                if (accessibilityFilter.stepFreeAccess()) {
                    predicates.add(hasAccessibilityFeature(root, query, criteriaBuilder,
                            AccessibilityFeatureKeys.STEP_FREE_ACCESS));
                }
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate hasAccessibilityFeature(Root<Event> root,
                                              jakarta.persistence.criteria.CriteriaQuery<?> query,
                                              jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
                                              String featureKey) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<AccessibilityPreference> accessibility = subquery.from(AccessibilityPreference.class);
        subquery.select(accessibility.get("id"))
                .where(criteriaBuilder.and(
                        criteriaBuilder.equal(accessibility.get("location").get("id"), root.get("location").get("id")),
                        criteriaBuilder.equal(accessibility.get("featureKey"), featureKey)
                ));
        return criteriaBuilder.exists(subquery);
    }

    public EventResponse getEventById(UUID id) {
        return toResponse(findEventById(id), true);
    }

    @Transactional
    public EventResponse updateEvent(UUID id, EventRequest request, Jwt jwt) {
        Event event = findEventById(id);
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        ensureEventOrganizer(event, currentUser);
        UUID previousLocationId = event.getLocation() != null ? event.getLocation().getId() : null;

        applyRequest(event, request);
        Event savedEvent = eventRepository.save(event);
        recommendationScoreService.recomputeEventScore(savedEvent.getId());
        recommendationScoreService.recomputePostScoresByEvent(savedEvent.getId());
        refreshChangedLocations(previousLocationId, savedEvent.getLocation() != null ? savedEvent.getLocation().getId() : null);
        notifyRegisteredUsers(savedEvent, currentUser, NotificationService.EVENT_UPDATED, "Event updated",
                savedEvent.getTitle() + " has been updated", "event:" + savedEvent.getId() + ":updated:" + System.currentTimeMillis());
        return toResponse(savedEvent, true);
    }

    @Transactional
    public void deleteEvent(UUID id, Jwt jwt) {
        Event event = findEventById(id);
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        ensureEventOrganizer(event, currentUser);
        UUID locationId = event.getLocation() != null ? event.getLocation().getId() : null;
        notifyRegisteredUsers(event, currentUser, NotificationService.EVENT_CANCELLED, "Event cancelled",
                event.getTitle() + " has been cancelled", "event:" + event.getId() + ":cancelled");
        eventRepository.delete(event);
        if (locationId != null) {
            recommendationScoreService.recomputeLocationScore(locationId);
        }
    }

    private void notifyRegisteredUsers(Event event,
                                       User actor,
                                       String type,
                                       String title,
                                       String body,
                                       String dedupeKey) {
        registrationRepository.findByEventIdOrderByRegisteredAtDesc(event.getId())
                .stream()
                .filter(registration -> isActiveRegistration(registration.getStatus()))
                .map(Registration::getUser)
                .forEach(user -> notificationService.create(
                        user,
                        actor,
                        type,
                        title,
                        body,
                        "EVENT",
                        event.getId(),
                        "EVENT",
                        event.getId(),
                        dedupeKey + ":" + user.getId(),
                        java.util.Map.of("eventId", event.getId().toString())
                ));
    }

    private boolean isActiveRegistration(String status) {
        if (status == null) {
            return true;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return !normalized.equals("CANCELLED") && !normalized.equals("REJECTED");
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

    private void ensureOrganizer(User user) {
        if (!isOrganizer(user) && !isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organizers or admins can manage events");
        }
    }

    private void ensureEventOrganizer(Event event, User currentUser) {
        ensureOrganizer(currentUser);
        if (isAdmin(currentUser)) {
            return;
        }
        if (event.getOrganizer() == null || !Objects.equals(event.getOrganizer().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event organizer can manage this event");
        }
    }

    private boolean isOrganizer(User user) {
        return user != null && ORGANIZER_ROLE.equalsIgnoreCase(user.getRole());
    }

    private boolean isAdmin(User user) {
        return user != null && ADMIN_ROLE.equalsIgnoreCase(user.getRole());
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

    private String normalizeOptionalFilter(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null || isAllOption(normalized)) {
            return null;
        }
        return normalized;
    }

    private List<String> normalizeOptionalFilters(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(this::normalizeOptionalFilter)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private UUID normalizeOptionalUuid(String value, String fieldName) {
        String normalized = normalizeOptionalFilter(value);
        if (normalized == null) {
            return null;
        }
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid UUID");
        }
    }

    private LocalDate normalizeOptionalDate(String value) {
        String normalized = normalizeOptionalFilter(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must use YYYY-MM-DD format");
        }
    }

    private AccessibilityFilter normalizeAccessibilityOptions(List<String> accessibilityOptions) {
        boolean wheelchairAccessible = false;
        boolean hasElevator = false;
        boolean accessibleToilet = false;
        boolean quietEnvironment = false;
        boolean stepFreeAccess = false;

        if (accessibilityOptions == null) {
            return new AccessibilityFilter(false, false, false, false, false);
        }

        for (String rawOption : accessibilityOptions) {
            if (rawOption == null || rawOption.isBlank()) {
                continue;
            }
            for (String splitOption : rawOption.split(",")) {
                String option = normalizeAccessibilityOption(splitOption);
                if (option == null) {
                    continue;
                }
                if (isAllOption(option)) {
                    continue;
                }
                switch (option) {
                    case "wheelchairaccessible" -> wheelchairAccessible = true;
                    case "haselevator", "elevator" -> hasElevator = true;
                    case "accessibletoilet", "toilet" -> accessibleToilet = true;
                    case "quietenvironment", "quiet" -> quietEnvironment = true;
                    case "stepfreeaccess", "stepfree" -> stepFreeAccess = true;
                    default -> throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Unknown accessibility option: " + splitOption.trim()
                    );
                }
            }
        }

        return new AccessibilityFilter(
                wheelchairAccessible,
                hasElevator,
                accessibleToilet,
                quietEnvironment,
                stepFreeAccess
        );
    }

    private String normalizeAccessibilityOption(String option) {
        if (option == null || option.isBlank()) {
            return null;
        }
        return option.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean isAllOption(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.equals("all")
                || normalized.equals("any")
                || normalized.equals("default")
                || normalized.equals("none");
    }

    private void refreshChangedLocations(UUID previousLocationId, UUID currentLocationId) {
        if (previousLocationId != null) {
            recommendationScoreService.recomputeLocationScore(previousLocationId);
        }
        if (currentLocationId != null && !Objects.equals(previousLocationId, currentLocationId)) {
            recommendationScoreService.recomputeLocationScore(currentLocationId);
        }
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

    private record AccessibilityFilter(boolean wheelchairAccessible,
                                       boolean hasElevator,
                                       boolean accessibleToilet,
                                       boolean quietEnvironment,
                                       boolean stepFreeAccess) {
        private boolean isEmpty() {
            return !wheelchairAccessible
                    && !hasElevator
                    && !accessibleToilet
                    && !quietEnvironment
                    && !stepFreeAccess;
        }
    }
}

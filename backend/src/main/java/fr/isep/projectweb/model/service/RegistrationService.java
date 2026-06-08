package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventTicketTierRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.RegistrationRequest;
import fr.isep.projectweb.model.dto.response.RegistrationResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventTicketTier;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class RegistrationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String EMAIL_PATTERN = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final EventTicketTierRepository eventTicketTierRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final BookingCredentialEmailService bookingCredentialEmailService;
    private final RecommendationScoreService recommendationScoreService;
    private final CalendarService calendarService;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               EventTicketTierRepository eventTicketTierRepository,
                               CurrentUserService currentUserService,
                               NotificationService notificationService,
                               BookingCredentialEmailService bookingCredentialEmailService,
                               RecommendationScoreService recommendationScoreService,
                               CalendarService calendarService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.eventTicketTierRepository = eventTicketTierRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.bookingCredentialEmailService = bookingCredentialEmailService;
        this.recommendationScoreService = recommendationScoreService;
        this.calendarService = calendarService;
    }

    public RegistrationResponse createRegistration(RegistrationRequest request, Jwt jwt) {
        Registration registration = new Registration();
        User user = currentUserService.getOrCreateCurrentUser(jwt);
        registration.setUser(user);
        applyRequest(registration, request);
        ensureNoActiveDuplicate(registration);
        Registration savedRegistration = registrationRepository.save(registration);
        Event event = savedRegistration.getEvent();
        recommendationScoreService.recomputeEventScore(event.getId());
        notificationService.create(
                event.getOrganizer(),
                savedRegistration.getUser(),
                NotificationService.EVENT_REGISTRATION_CREATED,
                "New event registration",
                savedRegistration.getUser().getFullName() + " registered for " + event.getTitle(),
                "EVENT",
                event.getId(),
                "REGISTRATION",
                savedRegistration.getId(),
                "registration:" + savedRegistration.getId() + ":created",
                java.util.Map.of(
                        "registrationId", savedRegistration.getId().toString(),
                        "eventId", event.getId().toString()
                )
        );
        if (isConfirmedStatus(savedRegistration.getStatus())) {
            bookingCredentialEmailService.sendBookingCredential(savedRegistration);
        }
        return ResponseMapper.toRegistrationResponse(savedRegistration);
    }

    public List<RegistrationResponse> getAllRegistrations(Jwt jwt) {
        ensureAdmin(currentUserService.getCurrentUser(jwt));
        return registrationRepository.findAll()
                .stream()
                .map(ResponseMapper::toRegistrationResponse)
                .toList();
    }

    public List<RegistrationResponse> getRegistrationsByEventId(UUID eventId, Jwt jwt) {
        Event event = findEventById(eventId);
        ensureCanManageEventRegistrations(event, currentUserService.getCurrentUser(jwt));
        return registrationRepository.findByEventIdOrderByRegisteredAtDesc(eventId)
                .stream()
                .map(ResponseMapper::toRegistrationResponse)
                .toList();
    }

    public List<RegistrationResponse> getRegistrationsByUserId(UUID userId, Jwt jwt) {
        User currentUser = currentUserService.getCurrentUser(jwt);
        if (!isAdmin(currentUser) && !Objects.equals(currentUser.getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own registrations");
        }
        return registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream()
                .map(ResponseMapper::toRegistrationResponse)
                .toList();
    }

    public RegistrationResponse getRegistrationById(UUID id, Jwt jwt) {
        Registration registration = findRegistrationById(id);
        ensureCanViewRegistration(registration, currentUserService.getCurrentUser(jwt));
        return ResponseMapper.toRegistrationResponse(registration);
    }

    public String getRegistrationCalendarIcs(UUID id, Jwt jwt) {
        Registration registration = findRegistrationById(id);
        ensureCanViewRegistration(registration, currentUserService.getCurrentUser(jwt));
        return calendarService.buildRegistrationIcs(registration);
    }

    public RegistrationResponse updateRegistration(UUID id, RegistrationRequest request, Jwt jwt) {
        Registration registration = findRegistrationById(id);
        ensureCanUpdateRegistration(registration, currentUserService.getCurrentUser(jwt));
        ensureSameEvent(registration, request);
        String previousStatus = registration.getStatus();
        applyRequest(registration, request);
        Registration savedRegistration = registrationRepository.save(registration);
        recommendationScoreService.recomputeEventScore(savedRegistration.getEvent().getId());
        if (!java.util.Objects.equals(previousStatus, savedRegistration.getStatus())) {
            Event event = savedRegistration.getEvent();
            notificationService.create(
                    savedRegistration.getUser(),
                    event.getOrganizer(),
                    NotificationService.EVENT_REGISTRATION_STATUS_CHANGED,
                    "Registration status updated",
                    "Your registration for " + event.getTitle() + " is now " + savedRegistration.getStatus(),
                    "REGISTRATION",
                    savedRegistration.getId(),
                    "REGISTRATION",
                    savedRegistration.getId(),
                    "registration:" + savedRegistration.getId() + ":status:" + savedRegistration.getStatus(),
                    java.util.Map.of(
                            "registrationId", savedRegistration.getId().toString(),
                            "eventId", event.getId().toString(),
                            "status", savedRegistration.getStatus() != null ? savedRegistration.getStatus() : ""
                    )
            );
            if (!isConfirmedStatus(previousStatus) && isConfirmedStatus(savedRegistration.getStatus())) {
                bookingCredentialEmailService.sendBookingCredential(savedRegistration);
            }
        }
        return ResponseMapper.toRegistrationResponse(savedRegistration);
    }

    public void deleteRegistration(UUID id, Jwt jwt) {
        Registration registration = findRegistrationById(id);
        ensureCanManageRegistration(registration, currentUserService.getCurrentUser(jwt));
        UUID eventId = registration.getEvent() != null ? registration.getEvent().getId() : null;
        bookingCredentialEmailService.sendBookingCancellation(registration);
        registrationRepository.delete(registration);
        if (eventId != null) {
            recommendationScoreService.recomputeEventScore(eventId);
        }
    }

    private void applyRequest(Registration registration, RegistrationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration payload is required");
        }
        Event event = findEventById(request.getEventId());
        registration.setEvent(event);
        registration.setStatus(normalizeStatus(request.getStatus()));
        applyTicketRequest(registration, request, event);
        applyContactRequest(registration, request);
    }

    private void applyTicketRequest(Registration registration, RegistrationRequest request, Event event) {
        EventTicketTier ticketTier = resolveTicketTier(request.getTicketTierId(), event, registration.getTicketTier());
        int quantity = request.getQuantity() != null ? request.getQuantity() : safeQuantity(registration.getQuantity());
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }
        validateTicketTier(ticketTier);
        validateCapacity(event, ticketTier, quantity, registration.getId());

        BigDecimal unitPrice = ticketTier != null && ticketTier.getPrice() != null
                ? ticketTier.getPrice()
                : safePrice(event.getPrice());
        registration.setTicketTier(ticketTier);
        registration.setQuantity(quantity);
        registration.setUnitPrice(unitPrice);
        registration.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        registration.setCurrency("SGD");
    }

    private EventTicketTier resolveTicketTier(UUID requestTierId, Event event, EventTicketTier existingTier) {
        if (requestTierId != null) {
            return eventTicketTierRepository.findByIdAndEventId(requestTierId, event.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket tier does not belong to this event"));
        }
        if (existingTier != null && Objects.equals(existingTier.getEvent().getId(), event.getId())) {
            return existingTier;
        }
        return eventTicketTierRepository.findFirstByEventIdAndActiveTrueOrderBySortOrderAscNameAsc(event.getId())
                .orElse(null);
    }

    private void validateTicketTier(EventTicketTier ticketTier) {
        if (ticketTier == null) {
            return;
        }
        if (!Boolean.TRUE.equals(ticketTier.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket tier is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (ticketTier.getSalesStartAt() != null && now.isBefore(ticketTier.getSalesStartAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket tier sales have not started");
        }
        if (ticketTier.getSalesEndAt() != null && now.isAfter(ticketTier.getSalesEndAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket tier sales have ended");
        }
    }

    private void validateCapacity(Event event, EventTicketTier ticketTier, int quantity, UUID excludeRegistrationId) {
        long eventSoldQuantity = registrationRepository.sumActiveQuantityByEventId(event.getId(), excludeRegistrationId);
        long eventCapacity = event.getCapacity() != null ? event.getCapacity() : 0L;
        if (eventCapacity > 0 && eventSoldQuantity + quantity > eventCapacity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough remaining event capacity");
        }
        if (ticketTier == null) {
            return;
        }
        long tierCapacity = ticketTier.getCapacity() != null ? ticketTier.getCapacity() : 0L;
        long tierSoldQuantity = registrationRepository.sumActiveQuantityByTicketTierId(ticketTier.getId(), excludeRegistrationId);
        if (tierCapacity > 0 && tierSoldQuantity + quantity > tierCapacity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough remaining ticket tier capacity");
        }
    }

    private void applyContactRequest(Registration registration, RegistrationRequest request) {
        boolean hasContactPayload = request.getContactFullName() != null
                || request.getContactEmail() != null
                || request.getContactPhone() != null;

        if (!hasContactPayload) {
            applyDefaultContactFromUser(registration);
            return;
        }

        if (request.getContactFullName() != null) {
            registration.setContactFullName(normalizeOptional(request.getContactFullName()));
        }
        if (request.getContactEmail() != null) {
            registration.setContactEmail(normalizeEmail(request.getContactEmail()));
        }
        if (request.getContactPhone() != null) {
            registration.setContactPhone(normalizeOptional(request.getContactPhone()));
        }
        applyDefaultContactFromUser(registration);
    }

    private void applyDefaultContactFromUser(Registration registration) {
        User user = registration.getUser();
        if (user == null) {
            return;
        }
        if (isBlank(registration.getContactFullName())) {
            registration.setContactFullName(normalizeOptional(user.getFullName()));
        }
        if (isBlank(registration.getContactEmail())) {
            registration.setContactEmail(normalizeEmail(user.getEmail()));
        }
        if (isBlank(registration.getContactPhone())) {
            registration.setContactPhone(normalizeOptional(user.getPhone()));
        }
    }

    private Registration findRegistrationById(UUID id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
    }

    private Event findEventById(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id must not be null");
        }

        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void ensureAdmin(User currentUser) {
        if (!isAdmin(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can view all registrations");
        }
    }

    private void ensureCanViewRegistration(Registration registration, User currentUser) {
        if (isAdmin(currentUser) || isRegistrationOwner(registration, currentUser) || isEventOrganizer(registration.getEvent(), currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view registrations you manage");
    }

    private void ensureCanManageRegistration(Registration registration, User currentUser) {
        if (isAdmin(currentUser) || isRegistrationOwner(registration, currentUser) || isEventOrganizer(registration.getEvent(), currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage registrations you own or organize");
    }

    private void ensureCanUpdateRegistration(Registration registration, User currentUser) {
        if (isAdmin(currentUser) || isEventOrganizer(registration.getEvent(), currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins or the event organizer can update registration status");
    }

    private void ensureSameEvent(Registration registration, RegistrationRequest request) {
        UUID currentEventId = registration.getEvent() != null ? registration.getEvent().getId() : null;
        if (!Objects.equals(currentEventId, request.getEventId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration event cannot be changed");
        }
    }

    private void ensureNoActiveDuplicate(Registration registration) {
        UUID eventId = registration.getEvent() != null ? registration.getEvent().getId() : null;
        UUID userId = registration.getUser() != null ? registration.getUser().getId() : null;
        if (eventId == null || userId == null) {
            return;
        }
        UUID ticketTierId = registration.getTicketTier() != null ? registration.getTicketTier().getId() : null;
        boolean hasActiveDuplicate = registrationRepository.findByEventIdAndUserIdOrderByRegisteredAtDesc(eventId, userId)
                .stream()
                .filter(existing -> !isInactiveRegistrationStatus(existing.getStatus()))
                .anyMatch(existing -> hasSameTicketTarget(existing, ticketTierId));
        if (hasActiveDuplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active registration for this ticket tier");
        }
    }

    private boolean hasSameTicketTarget(Registration existing, UUID ticketTierId) {
        UUID existingTicketTierId = existing.getTicketTier() != null ? existing.getTicketTier().getId() : null;
        if (ticketTierId == null) {
            return existingTicketTierId == null;
        }
        return Objects.equals(existingTicketTierId, ticketTierId);
    }

    private void ensureCanManageEventRegistrations(Event event, User currentUser) {
        if (isAdmin(currentUser) || isEventOrganizer(event, currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins or the event organizer can view event registrations");
    }

    private boolean isRegistrationOwner(Registration registration, User user) {
        return registration.getUser() != null && user != null && Objects.equals(registration.getUser().getId(), user.getId());
    }

    private boolean isEventOrganizer(Event event, User user) {
        return isOrganizer(user)
                && event != null
                && event.getOrganizer() != null
                && Objects.equals(event.getOrganizer().getId(), user.getId());
    }

    private boolean isOrganizer(User user) {
        return user != null && ORGANIZER_ROLE.equalsIgnoreCase(user.getRole());
    }

    private boolean isAdmin(User user) {
        return user != null && ADMIN_ROLE.equalsIgnoreCase(user.getRole());
    }

    private boolean isConfirmedStatus(String status) {
        return status != null && CONFIRMED_STATUS.equalsIgnoreCase(status.trim());
    }

    private boolean isInactiveRegistrationStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("CANCELLED") || normalized.equals("CANCELED") || normalized.equals("REJECTED");
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeOptional(status);
        return normalized != null ? normalized.toUpperCase(Locale.ROOT) : CONFIRMED_STATUS;
    }

    private int safeQuantity(Integer quantity) {
        return quantity != null ? quantity : 1;
    }

    private BigDecimal safePrice(BigDecimal price) {
        return price != null ? price : BigDecimal.ZERO;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeOptional(email);
        if (normalized == null) {
            return null;
        }
        if (!normalized.toUpperCase(java.util.Locale.ROOT).matches(EMAIL_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contact email is invalid");
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

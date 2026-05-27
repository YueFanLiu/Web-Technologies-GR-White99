package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.RegistrationRequest;
import fr.isep.projectweb.model.dto.response.RegistrationResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RegistrationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               CurrentUserService currentUserService,
                               NotificationService notificationService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    public RegistrationResponse createRegistration(RegistrationRequest request, Jwt jwt) {
        Registration registration = new Registration();
        registration.setUser(currentUserService.getOrCreateCurrentUser(jwt));
        applyRequest(registration, request);
        Registration savedRegistration = registrationRepository.save(registration);
        Event event = savedRegistration.getEvent();
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

    public RegistrationResponse updateRegistration(UUID id, RegistrationRequest request, Jwt jwt) {
        Registration registration = findRegistrationById(id);
        ensureCanUpdateRegistration(registration, currentUserService.getCurrentUser(jwt));
        ensureSameEvent(registration, request);
        String previousStatus = registration.getStatus();
        applyRequest(registration, request);
        Registration savedRegistration = registrationRepository.save(registration);
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
        }
        return ResponseMapper.toRegistrationResponse(savedRegistration);
    }

    public void deleteRegistration(UUID id, Jwt jwt) {
        Registration registration = findRegistrationById(id);
        ensureCanManageRegistration(registration, currentUserService.getCurrentUser(jwt));
        registrationRepository.delete(registration);
    }

    private void applyRequest(Registration registration, RegistrationRequest request) {
        registration.setEvent(findEventById(request.getEventId()));
        registration.setStatus(request.getStatus());
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
}

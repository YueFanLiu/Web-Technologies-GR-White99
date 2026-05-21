package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.RegistrationRequest;
import fr.isep.projectweb.model.dto.response.RegistrationResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Registration;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RegistrationService {

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

    public List<RegistrationResponse> getAllRegistrations() {
        return registrationRepository.findAll()
                .stream()
                .map(ResponseMapper::toRegistrationResponse)
                .toList();
    }

    public List<RegistrationResponse> getRegistrationsByEventId(UUID eventId) {
        return registrationRepository.findByEventIdOrderByRegisteredAtDesc(eventId)
                .stream()
                .map(ResponseMapper::toRegistrationResponse)
                .toList();
    }

    public List<RegistrationResponse> getRegistrationsByUserId(UUID userId) {
        return registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream()
                .map(ResponseMapper::toRegistrationResponse)
                .toList();
    }

    public RegistrationResponse getRegistrationById(UUID id) {
        return ResponseMapper.toRegistrationResponse(findRegistrationById(id));
    }

    public RegistrationResponse updateRegistration(UUID id, RegistrationRequest request) {
        Registration registration = findRegistrationById(id);
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

    public void deleteRegistration(UUID id) {
        Registration registration = findRegistrationById(id);
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
}

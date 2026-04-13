package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.RegistrationRequest;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;

    public RegistrationService(RegistrationRepository registrationRepository, EventRepository eventRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
    }

    public Registration createRegistration(RegistrationRequest request, User user) {
        Registration registration = new Registration();
        registration.setUser(user);
        applyRequest(registration, request);
        return registrationRepository.save(registration);
    }

    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    public Registration getRegistrationById(UUID id) {
        return findRegistrationById(id);
    }

    public Registration updateRegistration(UUID id, RegistrationRequest request) {
        Registration registration = findRegistrationById(id);
        applyRequest(registration, request);
        return registrationRepository.save(registration);
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

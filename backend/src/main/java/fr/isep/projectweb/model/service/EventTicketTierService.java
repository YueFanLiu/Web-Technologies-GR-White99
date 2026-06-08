package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventTicketTierRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.request.EventTicketTierRequest;
import fr.isep.projectweb.model.dto.response.EventTicketTierResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventTicketTier;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
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
public class EventTicketTierService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final EventTicketTierRepository eventTicketTierRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final CurrentUserService currentUserService;

    public EventTicketTierService(EventTicketTierRepository eventTicketTierRepository,
                                  EventRepository eventRepository,
                                  RegistrationRepository registrationRepository,
                                  CurrentUserService currentUserService) {
        this.eventTicketTierRepository = eventTicketTierRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.currentUserService = currentUserService;
    }

    public List<EventTicketTierResponse> listTicketTiers(UUID eventId) {
        findEventById(eventId);
        return eventTicketTierRepository.findByEventIdOrderBySortOrderAscNameAsc(eventId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EventTicketTierResponse createTicketTier(UUID eventId, EventTicketTierRequest request, Jwt jwt) {
        Event event = findEventById(eventId);
        ensureCanManageEvent(event, currentUserService.getCurrentUser(jwt));
        EventTicketTier ticketTier = new EventTicketTier();
        ticketTier.setEvent(event);
        applyRequest(ticketTier, request);
        return toResponse(eventTicketTierRepository.save(ticketTier));
    }

    public EventTicketTierResponse updateTicketTier(UUID eventId, UUID tierId, EventTicketTierRequest request, Jwt jwt) {
        Event event = findEventById(eventId);
        ensureCanManageEvent(event, currentUserService.getCurrentUser(jwt));
        EventTicketTier ticketTier = eventTicketTierRepository.findByIdAndEventId(tierId, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket tier not found"));
        applyRequest(ticketTier, request);
        return toResponse(eventTicketTierRepository.save(ticketTier));
    }

    public void deleteTicketTier(UUID eventId, UUID tierId, Jwt jwt) {
        Event event = findEventById(eventId);
        ensureCanManageEvent(event, currentUserService.getCurrentUser(jwt));
        EventTicketTier ticketTier = eventTicketTierRepository.findByIdAndEventId(tierId, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket tier not found"));
        if (registrationRepository.sumActiveQuantityByTicketTierId(tierId, null) > 0) {
            ticketTier.setActive(false);
            eventTicketTierRepository.save(ticketTier);
            return;
        }
        eventTicketTierRepository.delete(ticketTier);
    }

    EventTicketTierResponse toResponse(EventTicketTier ticketTier) {
        EventTicketTierResponse response = new EventTicketTierResponse();
        response.setId(ticketTier.getId());
        response.setEventId(ticketTier.getEvent() != null ? ticketTier.getEvent().getId() : null);
        response.setName(ticketTier.getName());
        response.setType(ticketTier.getType());
        response.setPrice(ticketTier.getPrice());
        response.setCapacity(ticketTier.getCapacity());
        response.setSalesStartAt(ticketTier.getSalesStartAt());
        response.setSalesEndAt(ticketTier.getSalesEndAt());
        response.setActive(ticketTier.getActive());
        response.setSortOrder(ticketTier.getSortOrder());
        response.setCreatedAt(ticketTier.getCreatedAt());
        response.setUpdatedAt(ticketTier.getUpdatedAt());
        long soldQuantity = registrationRepository.sumActiveQuantityByTicketTierId(ticketTier.getId(), null);
        response.setSoldQuantity(soldQuantity);
        response.setRemainingQuantity(Math.max(0L, safeCapacity(ticketTier.getCapacity()) - soldQuantity));
        return response;
    }

    private void applyRequest(EventTicketTier ticketTier, EventTicketTierRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket tier payload is required");
        }
        String name = normalizeRequired(request.getName(), "Ticket tier name is required");
        BigDecimal price = request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket tier price cannot be negative");
        }
        int capacity = request.getCapacity() != null ? request.getCapacity() : 0;
        if (capacity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket tier capacity cannot be negative");
        }
        LocalDateTime start = request.getSalesStartAt();
        LocalDateTime end = request.getSalesEndAt();
        if (start != null && end != null && end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket sales end time cannot be before start time");
        }

        ticketTier.setName(name);
        ticketTier.setType(normalizeType(request.getType()));
        ticketTier.setPrice(price);
        ticketTier.setCapacity(capacity);
        ticketTier.setSalesStartAt(start);
        ticketTier.setSalesEndAt(end);
        ticketTier.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);
        ticketTier.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
    }

    private Event findEventById(UUID eventId) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event id is required");
        }
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void ensureCanManageEvent(Event event, User currentUser) {
        if (isAdmin(currentUser) || isEventOrganizer(event, currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins or the event organizer can manage ticket tiers");
    }

    private boolean isAdmin(User user) {
        return user != null && ADMIN_ROLE.equalsIgnoreCase(user.getRole());
    }

    private boolean isEventOrganizer(Event event, User user) {
        return user != null
                && ORGANIZER_ROLE.equalsIgnoreCase(user.getRole())
                && event != null
                && event.getOrganizer() != null
                && Objects.equals(event.getOrganizer().getId(), user.getId());
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "STANDARD";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private long safeCapacity(Integer capacity) {
        return capacity != null ? capacity : 0L;
    }
}

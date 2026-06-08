package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.EventTicketTierRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dto.response.EventAnalyticsResponse;
import fr.isep.projectweb.model.dto.response.TicketTierAnalyticsResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventTicketTier;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventAnalyticsService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EventTicketTierRepository eventTicketTierRepository;
    private final EventReviewRepository eventReviewRepository;
    private final CurrentUserService currentUserService;

    public EventAnalyticsService(EventRepository eventRepository,
                                 RegistrationRepository registrationRepository,
                                 EventTicketTierRepository eventTicketTierRepository,
                                 EventReviewRepository eventReviewRepository,
                                 CurrentUserService currentUserService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.eventTicketTierRepository = eventTicketTierRepository;
        this.eventReviewRepository = eventReviewRepository;
        this.currentUserService = currentUserService;
    }

    public EventAnalyticsResponse getEventAnalytics(UUID eventId, Jwt jwt) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        ensureCanViewAnalytics(event, currentUserService.getCurrentUser(jwt));

        List<Registration> registrations = registrationRepository.findByEventIdOrderByRegisteredAtDesc(eventId);
        long confirmedCount = registrations.stream().filter(registration -> hasStatus(registration, "CONFIRMED")).count();
        long cancelledCount = registrations.stream().filter(this::isCancelled).count();
        long registeredCount = registrations.stream().filter(registration -> !isCancelled(registration)).count();
        long soldQuantity = registrations.stream()
                .filter(registration -> !isCancelled(registration))
                .mapToLong(this::safeQuantity)
                .sum();
        BigDecimal ticketSalesTotal = registrations.stream()
                .filter(registration -> !isCancelled(registration))
                .map(registration -> registration.getTotalPrice() != null ? registration.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        EventAnalyticsResponse response = new EventAnalyticsResponse();
        response.setEventId(eventId);
        response.setCapacity(event.getCapacity());
        response.setTotalRegistrations((long) registrations.size());
        response.setRegisteredCount(registeredCount);
        response.setConfirmedCount(confirmedCount);
        response.setCancelledCount(cancelledCount);
        response.setSoldQuantity(soldQuantity);
        response.setRemainingSpots(Math.max(0L, safeCapacity(event.getCapacity()) - soldQuantity));
        response.setTicketSalesTotal(ticketSalesTotal);
        response.setAverageRating(eventReviewRepository.averageRatingByEventId(eventId));
        response.setReviewCount(eventReviewRepository.countByEventId(eventId));
        response.setTicketTiers(buildTierAnalytics(eventId, registrations));
        return response;
    }

    private List<TicketTierAnalyticsResponse> buildTierAnalytics(UUID eventId, List<Registration> registrations) {
        return eventTicketTierRepository.findByEventIdOrderBySortOrderAscNameAsc(eventId)
                .stream()
                .map(ticketTier -> toTierAnalytics(ticketTier, registrations))
                .toList();
    }

    private TicketTierAnalyticsResponse toTierAnalytics(EventTicketTier ticketTier, List<Registration> registrations) {
        long soldQuantity = registrations.stream()
                .filter(registration -> registration.getTicketTier() != null)
                .filter(registration -> Objects.equals(registration.getTicketTier().getId(), ticketTier.getId()))
                .filter(registration -> !isCancelled(registration))
                .mapToLong(this::safeQuantity)
                .sum();
        BigDecimal revenue = registrations.stream()
                .filter(registration -> registration.getTicketTier() != null)
                .filter(registration -> Objects.equals(registration.getTicketTier().getId(), ticketTier.getId()))
                .filter(registration -> !isCancelled(registration))
                .map(registration -> registration.getTotalPrice() != null ? registration.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TicketTierAnalyticsResponse response = new TicketTierAnalyticsResponse();
        response.setTierId(ticketTier.getId());
        response.setName(ticketTier.getName());
        response.setType(ticketTier.getType());
        response.setPrice(ticketTier.getPrice());
        response.setCapacity(ticketTier.getCapacity());
        response.setSoldQuantity(soldQuantity);
        response.setRemainingQuantity(Math.max(0L, safeCapacity(ticketTier.getCapacity()) - soldQuantity));
        response.setRevenue(revenue);
        return response;
    }

    private void ensureCanViewAnalytics(Event event, User currentUser) {
        if (isAdmin(currentUser) || isEventOrganizer(event, currentUser)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins or the event organizer can view analytics");
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

    private boolean hasStatus(Registration registration, String status) {
        return registration.getStatus() != null && status.equalsIgnoreCase(registration.getStatus().trim());
    }

    private boolean isCancelled(Registration registration) {
        return hasStatus(registration, "CANCELLED")
                || hasStatus(registration, "CANCELED")
                || hasStatus(registration, "REJECTED");
    }

    private long safeQuantity(Registration registration) {
        return registration.getQuantity() != null ? registration.getQuantity() : 1L;
    }

    private long safeCapacity(Integer capacity) {
        return capacity != null ? capacity : 0L;
    }
}

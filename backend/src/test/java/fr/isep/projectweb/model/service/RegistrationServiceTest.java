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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventTicketTierRepository eventTicketTierRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BookingCredentialEmailService bookingCredentialEmailService;
    @Mock
    private RecommendationScoreService recommendationScoreService;
    @Mock
    private CalendarService calendarService;

    private RegistrationService registrationService;
    private User attendee;
    private User organizer;
    private Event event;
    private EventTicketTier standardTier;
    private EventTicketTier vipTier;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                registrationRepository,
                eventRepository,
                eventTicketTierRepository,
                currentUserService,
                notificationService,
                bookingCredentialEmailService,
                recommendationScoreService,
                calendarService
        );

        attendee = user(UUID.randomUUID(), "attendee@example.com", "Attendee", "PARENT");
        organizer = user(UUID.randomUUID(), "organizer@example.com", "Organizer", "ORGANIZER");

        event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Inclusive Workshop");
        event.setDescription("Accessible activity");
        event.setOrganizer(organizer);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        event.setCapacity(20);
        event.setPrice(BigDecimal.TEN);

        standardTier = ticketTier("Standard", "STANDARD", BigDecimal.valueOf(30), 10);
        vipTier = ticketTier("VIP", "VIP", BigDecimal.valueOf(80), 5);
    }

    @Test
    void createsRegistrationWithTicketQuantityAndPriceSnapshot() {
        RegistrationRequest request = request(standardTier.getId(), 2, "CONFIRMED");
        mockSuccessfulCreate(request, List.of());

        RegistrationResponse response = registrationService.createRegistration(request, null);

        assertNotNull(response.getId());
        assertEquals(standardTier.getId(), response.getTicketTierId());
        assertEquals(2, response.getQuantity());
        assertEquals(0, BigDecimal.valueOf(30).compareTo(response.getUnitPrice()));
        assertEquals(0, BigDecimal.valueOf(60).compareTo(response.getTotalPrice()));
        verify(bookingCredentialEmailService).sendBookingCredential(any(Registration.class));
        verify(recommendationScoreService).recomputeEventScore(event.getId());
        verify(notificationService).create(
                eq(organizer),
                eq(attendee),
                eq(NotificationService.EVENT_REGISTRATION_CREATED),
                anyString(),
                anyString(),
                anyString(),
                eq(event.getId()),
                anyString(),
                any(UUID.class),
                anyString(),
                anyMap()
        );
    }

    @Test
    void allowsSameUserToBookDifferentTicketTiersForSameEvent() {
        Registration existingVipRegistration = registration(vipTier, attendee, event, "CONFIRMED");
        RegistrationRequest request = request(standardTier.getId(), 1, "CONFIRMED");
        mockSuccessfulCreate(request, List.of(existingVipRegistration));

        RegistrationResponse response = registrationService.createRegistration(request, null);

        assertEquals(standardTier.getId(), response.getTicketTierId());
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    void rejectsDuplicateActiveRegistrationForSameTicketTier() {
        Registration existingStandardRegistration = registration(standardTier, attendee, event, "CONFIRMED");
        RegistrationRequest request = request(standardTier.getId(), 1, "CONFIRMED");
        mockCreateDependencies(request, List.of(existingStandardRegistration));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.createRegistration(request, null)
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void rejectsRegistrationWhenTicketTierCapacityIsExceeded() {
        RegistrationRequest request = request(standardTier.getId(), 3, "CONFIRMED");
        when(currentUserService.getOrCreateCurrentUser(null)).thenReturn(attendee);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventTicketTierRepository.findByIdAndEventId(standardTier.getId(), event.getId())).thenReturn(Optional.of(standardTier));
        when(registrationRepository.sumActiveQuantityByEventId(event.getId(), null)).thenReturn(0L);
        when(registrationRepository.sumActiveQuantityByTicketTierId(standardTier.getId(), null)).thenReturn(9L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationService.createRegistration(request, null)
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void deletesRegistrationAndSendsCancellationEmail() {
        Registration registration = registration(standardTier, attendee, event, "CONFIRMED");
        registration.setId(UUID.randomUUID());
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(currentUserService.getCurrentUser(null)).thenReturn(attendee);

        registrationService.deleteRegistration(registration.getId(), null);

        verify(bookingCredentialEmailService).sendBookingCancellation(registration);
        verify(registrationRepository).delete(registration);
        verify(recommendationScoreService).recomputeEventScore(event.getId());
    }

    private void mockSuccessfulCreate(RegistrationRequest request, List<Registration> existingRegistrations) {
        mockCreateDependencies(request, existingRegistrations);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration registration = invocation.getArgument(0);
            registration.setId(UUID.randomUUID());
            return registration;
        });
    }

    private void mockCreateDependencies(RegistrationRequest request, List<Registration> existingRegistrations) {
        when(currentUserService.getOrCreateCurrentUser(null)).thenReturn(attendee);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventTicketTierRepository.findByIdAndEventId(request.getTicketTierId(), event.getId())).thenReturn(Optional.of(standardTier));
        when(registrationRepository.sumActiveQuantityByEventId(event.getId(), null)).thenReturn(0L);
        when(registrationRepository.sumActiveQuantityByTicketTierId(standardTier.getId(), null)).thenReturn(0L);
        when(registrationRepository.findByEventIdAndUserIdOrderByRegisteredAtDesc(event.getId(), attendee.getId()))
                .thenReturn(existingRegistrations);
    }

    private RegistrationRequest request(UUID ticketTierId, int quantity, String status) {
        RegistrationRequest request = new RegistrationRequest();
        request.setEventId(event.getId());
        request.setTicketTierId(ticketTierId);
        request.setQuantity(quantity);
        request.setStatus(status);
        request.setContactFullName("Booking Contact");
        request.setContactEmail("booking@example.com");
        request.setContactPhone("12345678");
        return request;
    }

    private EventTicketTier ticketTier(String name, String type, BigDecimal price, int capacity) {
        EventTicketTier ticketTier = new EventTicketTier();
        ticketTier.setId(UUID.randomUUID());
        ticketTier.setEvent(event);
        ticketTier.setName(name);
        ticketTier.setType(type);
        ticketTier.setPrice(price);
        ticketTier.setCapacity(capacity);
        ticketTier.setActive(true);
        ticketTier.setSortOrder(0);
        return ticketTier;
    }

    private Registration registration(EventTicketTier ticketTier, User user, Event event, String status) {
        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setUser(user);
        registration.setStatus(status);
        registration.setTicketTier(ticketTier);
        registration.setQuantity(1);
        registration.setUnitPrice(ticketTier.getPrice());
        registration.setTotalPrice(ticketTier.getPrice());
        registration.setCurrency("SGD");
        return registration;
    }

    private User user(UUID id, String email, String fullName, String role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }
}

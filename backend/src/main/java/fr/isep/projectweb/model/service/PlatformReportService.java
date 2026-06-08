package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dao.PostReviewRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.response.PlatformUsageReportResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlatformReportService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PostRepository postRepository;
    private final RegistrationRepository registrationRepository;
    private final EventReviewRepository eventReviewRepository;
    private final PostReviewRepository postReviewRepository;
    private final CurrentUserService currentUserService;

    public PlatformReportService(UserRepository userRepository,
                                 EventRepository eventRepository,
                                 PostRepository postRepository,
                                 RegistrationRepository registrationRepository,
                                 EventReviewRepository eventReviewRepository,
                                 PostReviewRepository postReviewRepository,
                                 CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.postRepository = postRepository;
        this.registrationRepository = registrationRepository;
        this.eventReviewRepository = eventReviewRepository;
        this.postReviewRepository = postReviewRepository;
        this.currentUserService = currentUserService;
    }

    public PlatformUsageReportResponse getPlatformUsage(LocalDate from, LocalDate to, Jwt jwt) {
        ensureAdmin(currentUserService.getCurrentUser(jwt));
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;

        List<User> users = userRepository.findAll();
        List<Event> events = eventRepository.findAll().stream().filter(event -> inRange(event.getCreatedAt(), fromDateTime, toDateTime)).toList();
        List<Registration> registrations = registrationRepository.findAll().stream().filter(registration -> inRange(registration.getRegisteredAt(), fromDateTime, toDateTime)).toList();

        PlatformUsageReportResponse response = new PlatformUsageReportResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setTotalUsers((long) users.size());
        response.setTotalEvents((long) events.size());
        response.setTotalPosts(postRepository.count());
        response.setTotalRegistrations((long) registrations.size());
        response.setTicketSalesTotal(sumRevenue(registrations));
        response.setAverageFeedbackRating(calculateAverageFeedbackRating());
        response.setUsersByRole(toMetricCounts(users, user -> safeLabel(user.getRole())));
        response.setUsersByStatus(toMetricCounts(users, user -> user.getStatus() != null ? user.getStatus() : "ACTIVE"));
        response.setTopEventsByRegistrations(topEventsByRegistrations(registrations));
        response.setTopEventsByRevenue(topEventsByRevenue(registrations));
        response.setTopEventsByRating(topEventsByRating(eventRepository.findAll()));
        return response;
    }

    private List<PlatformUsageReportResponse.MetricCountResponse> toMetricCounts(List<User> users,
                                                                                Function<User, String> classifier) {
        return users.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    PlatformUsageReportResponse.MetricCountResponse response = new PlatformUsageReportResponse.MetricCountResponse();
                    response.setLabel(entry.getKey());
                    response.setCount(entry.getValue());
                    return response;
                })
                .toList();
    }

    private List<PlatformUsageReportResponse.TopEventMetricResponse> topEventsByRegistrations(List<Registration> registrations) {
        return registrations.stream()
                .filter(registration -> registration.getEvent() != null)
                .collect(Collectors.groupingBy(registration -> registration.getEvent().getId(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> topEventMetric(entry.getKey(), entry.getValue(), null, null))
                .toList();
    }

    private List<PlatformUsageReportResponse.TopEventMetricResponse> topEventsByRevenue(List<Registration> registrations) {
        return registrations.stream()
                .filter(registration -> registration.getEvent() != null)
                .collect(Collectors.groupingBy(registration -> registration.getEvent().getId(), Collectors.reducing(BigDecimal.ZERO,
                        registration -> registration.getTotalPrice() != null ? registration.getTotalPrice() : BigDecimal.ZERO,
                        BigDecimal::add)))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(entry -> topEventMetric(entry.getKey(), null, entry.getValue(), null))
                .toList();
    }

    private List<PlatformUsageReportResponse.TopEventMetricResponse> topEventsByRating(List<Event> events) {
        return events.stream()
                .map(event -> topEventMetric(event.getId(), null, null, eventReviewRepository.averageRatingByEventId(event.getId())))
                .filter(metric -> metric.getRating() != null)
                .sorted(Comparator.comparing(PlatformUsageReportResponse.TopEventMetricResponse::getRating).reversed())
                .limit(5)
                .toList();
    }

    private PlatformUsageReportResponse.TopEventMetricResponse topEventMetric(UUID eventId, Long count, BigDecimal revenue, Double rating) {
        Event event = eventRepository.findById(eventId).orElse(null);
        PlatformUsageReportResponse.TopEventMetricResponse response = new PlatformUsageReportResponse.TopEventMetricResponse();
        response.setEventId(eventId);
        response.setTitle(event != null ? event.getTitle() : "");
        response.setCount(count);
        response.setRevenue(revenue);
        response.setRating(rating);
        return response;
    }

    private BigDecimal sumRevenue(List<Registration> registrations) {
        return registrations.stream()
                .map(registration -> registration.getTotalPrice() != null ? registration.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double calculateAverageFeedbackRating() {
        List<Double> ratings = eventRepository.findAll().stream()
                .map(event -> eventReviewRepository.averageRatingByEventId(event.getId()))
                .filter(rating -> rating != null)
                .collect(Collectors.toList());
        ratings.addAll(postRepository.findAll().stream()
                .map(post -> postReviewRepository.averageRatingByPostId(post.getId()))
                .filter(rating -> rating != null)
                .toList());
        if (ratings.isEmpty()) {
            return null;
        }
        return ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) {
            return true;
        }
        return (from == null || !value.isBefore(from)) && (to == null || value.isBefore(to));
    }

    private void ensureAdmin(User user) {
        if (user == null || !ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can view reports");
        }
    }

    private String safeLabel(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.toUpperCase(Locale.ROOT);
    }
}

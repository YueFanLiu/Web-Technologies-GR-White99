package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class BookingCredentialEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingCredentialEmailService.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;
    private final String mailUsername;

    public BookingCredentialEmailService(JavaMailSender mailSender,
                                         @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
    }

    public void sendBookingCredential(Registration registration) {
        User user = registration.getUser();
        Event event = registration.getEvent();
        if (event == null) {
            return;
        }
        sendRegistrationEmail(
                registration,
                user,
                event,
                "Your booking credential for " + event.getTitle(),
                buildCredentialEmailBody(registration, event, user),
                "Booking credential email"
        );
    }

    public void sendBookingCancellation(Registration registration) {
        User user = registration.getUser();
        Event event = registration.getEvent();
        if (event == null) {
            return;
        }
        sendRegistrationEmail(
                registration,
                user,
                event,
                "Your booking has been cancelled for " + event.getTitle(),
                buildCancellationEmailBody(registration, event, user),
                "Booking cancellation email"
        );
    }

    private void sendRegistrationEmail(Registration registration,
                                       User user,
                                       Event event,
                                       String subject,
                                       String body,
                                       String logLabel) {
        String recipientEmail = firstNonBlank(registration.getContactEmail(), user != null ? user.getEmail() : null);
        if (recipientEmail == null) {
            return;
        }
        if (mailUsername == null || mailUsername.isBlank()) {
            log.info("{} skipped because MAIL_USERNAME is not configured", logLabel);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("{} could not be sent for registration {}", logLabel, registration.getId(), exception);
        }
    }

    private String buildCredentialEmailBody(Registration registration, Event event, User user) {
        return """
                Hello %s,

                Your booking is confirmed. Please keep this email as your activity credential.

                Booking ID: %s
                Activity: %s
                Status: %s
                Ticket: %s
                Quantity: %s
                Total: %s
                Start: %s
                End: %s
                Location: %s

                Show this booking ID if the organizer asks for confirmation.
                """.formatted(
                firstNonBlank(registration.getContactFullName(), user != null ? user.getFullName() : null, "there"),
                registration.getId(),
                event.getTitle(),
                registration.getStatus() != null ? registration.getStatus() : "CONFIRMED",
                ticketName(registration),
                registration.getQuantity() != null ? registration.getQuantity() : 1,
                formatMoney(registration),
                event.getStartTime() != null ? event.getStartTime().format(DATE_TIME_FORMAT) : "TBA",
                event.getEndTime() != null ? event.getEndTime().format(DATE_TIME_FORMAT) : "TBA",
                formatLocation(event)
        );
    }

    private String buildCancellationEmailBody(Registration registration, Event event, User user) {
        return """
                Hello %s,

                Your booking has been cancelled and removed from our system.

                Cancelled Booking ID: %s
                Activity: %s
                Ticket: %s
                Quantity: %s
                Total: %s
                Start: %s
                End: %s
                Location: %s

                You no longer need to attend this activity under this booking.
                """.formatted(
                firstNonBlank(registration.getContactFullName(), user != null ? user.getFullName() : null, "there"),
                registration.getId(),
                event.getTitle(),
                ticketName(registration),
                registration.getQuantity() != null ? registration.getQuantity() : 1,
                formatMoney(registration),
                event.getStartTime() != null ? event.getStartTime().format(DATE_TIME_FORMAT) : "TBA",
                event.getEndTime() != null ? event.getEndTime().format(DATE_TIME_FORMAT) : "TBA",
                formatLocation(event)
        );
    }

    private String ticketName(Registration registration) {
        return registration.getTicketTier() != null ? registration.getTicketTier().getName() : "Standard";
    }

    private String formatMoney(Registration registration) {
        java.math.BigDecimal total = registration.getTotalPrice() != null ? registration.getTotalPrice() : java.math.BigDecimal.ZERO;
        if (total.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "Free";
        }
        return (registration.getCurrency() != null ? registration.getCurrency() : "SGD") + " " + total;
    }

    private String formatLocation(Event event) {
        Location location = event.getLocation();
        if (location == null) {
            return "Location TBA";
        }
        String locationText = String.join(", ",
                java.util.stream.Stream.of(location.getName(), location.getAddress(), location.getCity(), location.getCountry())
                        .filter(value -> value != null && !value.isBlank())
                        .toList());
        return locationText == null || locationText.isBlank() ? "Location TBA" : locationText;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}

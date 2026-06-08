package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Registration;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class CalendarService {

    private static final DateTimeFormatter ICS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public String buildRegistrationIcs(Registration registration) {
        Event event = registration.getEvent();
        String uid = "access4all-registration-" + registration.getId() + "@access4all";
        String title = event != null ? event.getTitle() : "Access4All Event";
        String description = event != null ? event.getDescription() : "";
        String location = buildLocation(event != null ? event.getLocation() : null);

        return String.join("\r\n",
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//Access4All//Booking Calendar//EN",
                "CALSCALE:GREGORIAN",
                "METHOD:PUBLISH",
                "BEGIN:VEVENT",
                "UID:" + escape(uid),
                "DTSTAMP:" + formatUtc(java.time.LocalDateTime.now()),
                "DTSTART:" + formatUtc(event.getStartTime()),
                "DTEND:" + formatUtc(event.getEndTime()),
                "SUMMARY:" + escape(title),
                "DESCRIPTION:" + escape(description),
                "LOCATION:" + escape(location),
                "END:VEVENT",
                "END:VCALENDAR",
                ""
        );
    }

    private String buildLocation(Location location) {
        if (location == null) {
            return "";
        }
        return java.util.stream.Stream.of(location.getName(), location.getAddress(), location.getCity(), location.getCountry())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String formatUtc(java.time.LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(ICS_FORMATTER);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}

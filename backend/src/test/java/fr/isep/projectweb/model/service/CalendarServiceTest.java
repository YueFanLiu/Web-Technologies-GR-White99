package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Registration;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarServiceTest {

    private final CalendarService calendarService = new CalendarService();

    @Test
    void buildsRegistrationIcsWithCoreEventFields() {
        Registration registration = registration("Accessible Music Day", "Friendly activity");

        String ics = calendarService.buildRegistrationIcs(registration);

        assertTrue(ics.contains("BEGIN:VCALENDAR"));
        assertTrue(ics.contains("BEGIN:VEVENT"));
        assertTrue(ics.contains("SUMMARY:Accessible Music Day"));
        assertTrue(ics.contains("DTSTART:"));
        assertTrue(ics.contains("DTEND:"));
        assertTrue(ics.contains("LOCATION:Inclusive Center\\, 12 Main St\\, Singapore\\, SG"));
    }

    @Test
    void escapesSpecialCharactersForIcs() {
        Registration registration = registration("Music, Art; Access", "Line one\nLine two");

        String ics = calendarService.buildRegistrationIcs(registration);

        assertTrue(ics.contains("SUMMARY:Music\\, Art\\; Access"));
        assertTrue(ics.contains("DESCRIPTION:Line one\\nLine two"));
    }

    private Registration registration(String title, String description) {
        Location location = new Location();
        location.setName("Inclusive Center");
        location.setAddress("12 Main St");
        location.setCity("Singapore");
        location.setCountry("SG");

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle(title);
        event.setDescription(description);
        event.setStartTime(LocalDateTime.of(2026, 6, 9, 9, 0));
        event.setEndTime(LocalDateTime.of(2026, 6, 9, 11, 0));
        event.setLocation(location);

        Registration registration = new Registration();
        registration.setId(UUID.randomUUID());
        registration.setEvent(event);
        return registration;
    }
}

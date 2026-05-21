package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Registration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EventReminderService {

    private static final int SCAN_WINDOW_MINUTES = 5;

    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;

    public EventReminderService(RegistrationRepository registrationRepository,
                                NotificationService notificationService) {
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 300000)
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        sendRemindersForWindow(
                now.plusDays(1),
                NotificationService.EVENT_STARTS_IN_1_DAY,
                "Event starts tomorrow",
                "1_DAY"
        );
        sendRemindersForWindow(
                now.plusHours(2),
                NotificationService.EVENT_STARTS_IN_2_HOURS,
                "Event starts soon",
                "2_HOURS"
        );
    }

    private void sendRemindersForWindow(LocalDateTime targetTime,
                                        String type,
                                        String title,
                                        String reminder) {
        LocalDateTime from = targetTime;
        LocalDateTime to = targetTime.plusMinutes(SCAN_WINDOW_MINUTES);
        registrationRepository.findReminderTargets(from, to)
                .forEach(registration -> createReminder(registration, type, title, reminder));
    }

    private void createReminder(Registration registration, String type, String title, String reminder) {
        Event event = registration.getEvent();
        String body = event.getTitle() + ("1_DAY".equals(reminder) ? " starts in 1 day" : " starts in 2 hours");
        notificationService.create(
                registration.getUser(),
                null,
                type,
                title,
                body,
                "EVENT",
                event.getId(),
                "EVENT",
                event.getId(),
                "event:" + event.getId() + ":starts:" + reminder + ":" + event.getStartTime(),
                Map.of(
                        "eventId", event.getId().toString(),
                        "eventTitle", event.getTitle(),
                        "startTime", event.getStartTime().toString(),
                        "reminder", reminder
                )
        );
    }
}

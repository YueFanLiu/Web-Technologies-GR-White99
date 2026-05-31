package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.EventSaveRepository;
import fr.isep.projectweb.model.dto.response.EventSaveResponse;
import fr.isep.projectweb.model.dto.response.EventSaveStatusResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventSave;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class EventSaveService {

    private final EventSaveRepository eventSaveRepository;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final RecommendationScoreService recommendationScoreService;

    public EventSaveService(EventSaveRepository eventSaveRepository,
                            EventRepository eventRepository,
                            CurrentUserService currentUserService,
                            RecommendationScoreService recommendationScoreService) {
        this.eventSaveRepository = eventSaveRepository;
        this.eventRepository = eventRepository;
        this.currentUserService = currentUserService;
        this.recommendationScoreService = recommendationScoreService;
    }

    @Transactional
    public EventSaveResponse saveEvent(UUID eventId, Jwt jwt) {
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        Event event = findEvent(eventId);

        return eventSaveRepository.findFirstByUserIdAndEventId(currentUser.getId(), eventId)
                .map(ResponseMapper::toEventSaveResponse)
                .orElseGet(() -> {
                    EventSave eventSave = new EventSave();
                    eventSave.setUser(currentUser);
                    eventSave.setEvent(event);
                    EventSave saved = eventSaveRepository.save(eventSave);
                    recommendationScoreService.recomputeEventScore(eventId);
                    return ResponseMapper.toEventSaveResponse(saved);
                });
    }

    public EventSaveStatusResponse getSaveStatus(UUID eventId, Jwt jwt) {
        findEvent(eventId);
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return new EventSaveStatusResponse(
                eventId,
                eventSaveRepository.existsByUserIdAndEventId(currentUserId, eventId)
        );
    }

    public List<EventSaveResponse> getMySavedEvents(Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return eventSaveRepository.findByUserIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(ResponseMapper::toEventSaveResponse)
                .toList();
    }

    @Transactional
    public void unsaveEvent(UUID eventId, Jwt jwt) {
        findEvent(eventId);
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        eventSaveRepository.deleteByUserIdAndEventId(currentUserId, eventId);
        recommendationScoreService.recomputeEventScore(eventId);
    }

    private Event findEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }
}

package fr.isep.projectweb.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class EventSaveResponse {

    private UUID id;
    private EventSummaryResponse event;
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EventSummaryResponse getEvent() {
        return event;
    }

    public void setEvent(EventSummaryResponse event) {
        this.event = event;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

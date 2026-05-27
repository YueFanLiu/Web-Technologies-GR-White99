package fr.isep.projectweb.model.dto.response;

import java.util.UUID;

public class EventSaveStatusResponse {

    private UUID eventId;
    private boolean saved;

    public EventSaveStatusResponse(UUID eventId, boolean saved) {
        this.eventId = eventId;
        this.saved = saved;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }
}

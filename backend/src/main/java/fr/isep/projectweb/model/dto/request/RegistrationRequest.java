package fr.isep.projectweb.model.dto.request;

import java.util.UUID;

public class RegistrationRequest {

    private UUID eventId;
    private String status;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

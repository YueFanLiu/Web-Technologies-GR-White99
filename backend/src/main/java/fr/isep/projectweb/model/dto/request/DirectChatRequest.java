package fr.isep.projectweb.model.dto.request;

import java.util.UUID;

public class DirectChatRequest {

    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}

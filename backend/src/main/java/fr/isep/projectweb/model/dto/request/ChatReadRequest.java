package fr.isep.projectweb.model.dto.request;

import java.util.UUID;

public class ChatReadRequest {

    private UUID messageId;

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }
}

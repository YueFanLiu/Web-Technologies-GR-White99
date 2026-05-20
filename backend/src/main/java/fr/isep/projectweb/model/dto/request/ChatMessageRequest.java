package fr.isep.projectweb.model.dto.request;

public class ChatMessageRequest {

    private String content;
    private String messageType;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}

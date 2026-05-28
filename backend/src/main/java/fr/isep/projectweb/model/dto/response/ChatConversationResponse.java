package fr.isep.projectweb.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ChatConversationResponse {

    private UUID id;
    private String type;
    private EventSummaryResponse event;
    private List<PublicUserResponse> participants;
    private ChatMessageResponse lastMessage;
    private long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EventSummaryResponse getEvent() {
        return event;
    }

    public void setEvent(EventSummaryResponse event) {
        this.event = event;
    }

    public List<PublicUserResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(List<PublicUserResponse> participants) {
        this.participants = participants;
    }

    public ChatMessageResponse getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(ChatMessageResponse lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

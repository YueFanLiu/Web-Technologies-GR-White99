package fr.isep.projectweb.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class FriendRequestResponse {

    private UUID id;
    private PublicUserResponse requester;
    private PublicUserResponse addressee;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PublicUserResponse getRequester() {
        return requester;
    }

    public void setRequester(PublicUserResponse requester) {
        this.requester = requester;
    }

    public PublicUserResponse getAddressee() {
        return addressee;
    }

    public void setAddressee(PublicUserResponse addressee) {
        this.addressee = addressee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

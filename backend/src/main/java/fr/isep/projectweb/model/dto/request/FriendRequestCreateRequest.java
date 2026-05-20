package fr.isep.projectweb.model.dto.request;

import java.util.UUID;

public class FriendRequestCreateRequest {

    private UUID addresseeId;

    public UUID getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(UUID addresseeId) {
        this.addresseeId = addresseeId;
    }
}

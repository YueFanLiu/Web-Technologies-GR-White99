package fr.isep.projectweb.model.dto.response;

import java.time.LocalDateTime;

public class FriendResponse {

    private PublicUserResponse user;
    private LocalDateTime friendsSince;

    public PublicUserResponse getUser() {
        return user;
    }

    public void setUser(PublicUserResponse user) {
        this.user = user;
    }

    public LocalDateTime getFriendsSince() {
        return friendsSince;
    }

    public void setFriendsSince(LocalDateTime friendsSince) {
        this.friendsSince = friendsSince;
    }
}

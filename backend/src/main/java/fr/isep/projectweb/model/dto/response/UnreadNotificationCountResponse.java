package fr.isep.projectweb.model.dto.response;

public class UnreadNotificationCountResponse {

    private long count;

    public UnreadNotificationCountResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

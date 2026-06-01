package fr.isep.projectweb.model.dto.response;

import java.util.List;

public class FriendRecommendationResponse {

    private PublicUserResponse user;
    private double score;
    private int mutualFriendCount;
    private int sharedEventCount;
    private int sharedSavedEventCount;
    private int sharedReviewCount;
    private int accessibilityMatchCount;
    private List<String> reasons;

    public PublicUserResponse getUser() {
        return user;
    }

    public void setUser(PublicUserResponse user) {
        this.user = user;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getMutualFriendCount() {
        return mutualFriendCount;
    }

    public void setMutualFriendCount(int mutualFriendCount) {
        this.mutualFriendCount = mutualFriendCount;
    }

    public int getSharedEventCount() {
        return sharedEventCount;
    }

    public void setSharedEventCount(int sharedEventCount) {
        this.sharedEventCount = sharedEventCount;
    }

    public int getSharedSavedEventCount() {
        return sharedSavedEventCount;
    }

    public void setSharedSavedEventCount(int sharedSavedEventCount) {
        this.sharedSavedEventCount = sharedSavedEventCount;
    }

    public int getSharedReviewCount() {
        return sharedReviewCount;
    }

    public void setSharedReviewCount(int sharedReviewCount) {
        this.sharedReviewCount = sharedReviewCount;
    }

    public int getAccessibilityMatchCount() {
        return accessibilityMatchCount;
    }

    public void setAccessibilityMatchCount(int accessibilityMatchCount) {
        this.accessibilityMatchCount = accessibilityMatchCount;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}

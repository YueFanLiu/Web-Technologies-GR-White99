package fr.isep.projectweb.model.algorithm.recommendation.friend;

/*
 * Feature object for friend recommendations.
 *
 * The service layer collects graph and activity signals for one candidate user,
 * then this plain object is passed to FriendRecommendationScorer. Keeping the
 * algorithm inputs separate from JPA entities makes the weights easy to tune
 * without changing database mappings or API DTOs.
 */
public class FriendRecommendationFeatures {

    private int mutualFriendCount;
    private int sharedEventCount;
    private int sharedSavedEventCount;
    private int sharedReviewTargetCount;
    private boolean wheelchairAccessibleMatch;
    private boolean elevatorMatch;
    private boolean accessibleRestroomMatch;
    private boolean quietEnvironmentMatch;
    private String currentUserRole;
    private String candidateRole;
    private boolean candidateHasPhoto;
    private boolean candidateHasFullName;

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

    public int getSharedReviewTargetCount() {
        return sharedReviewTargetCount;
    }

    public void setSharedReviewTargetCount(int sharedReviewTargetCount) {
        this.sharedReviewTargetCount = sharedReviewTargetCount;
    }

    public boolean isWheelchairAccessibleMatch() {
        return wheelchairAccessibleMatch;
    }

    public void setWheelchairAccessibleMatch(boolean wheelchairAccessibleMatch) {
        this.wheelchairAccessibleMatch = wheelchairAccessibleMatch;
    }

    public boolean isElevatorMatch() {
        return elevatorMatch;
    }

    public void setElevatorMatch(boolean elevatorMatch) {
        this.elevatorMatch = elevatorMatch;
    }

    public boolean isAccessibleRestroomMatch() {
        return accessibleRestroomMatch;
    }

    public void setAccessibleRestroomMatch(boolean accessibleRestroomMatch) {
        this.accessibleRestroomMatch = accessibleRestroomMatch;
    }

    public boolean isQuietEnvironmentMatch() {
        return quietEnvironmentMatch;
    }

    public void setQuietEnvironmentMatch(boolean quietEnvironmentMatch) {
        this.quietEnvironmentMatch = quietEnvironmentMatch;
    }

    public String getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(String currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    public String getCandidateRole() {
        return candidateRole;
    }

    public void setCandidateRole(String candidateRole) {
        this.candidateRole = candidateRole;
    }

    public boolean isCandidateHasPhoto() {
        return candidateHasPhoto;
    }

    public void setCandidateHasPhoto(boolean candidateHasPhoto) {
        this.candidateHasPhoto = candidateHasPhoto;
    }

    public boolean isCandidateHasFullName() {
        return candidateHasFullName;
    }

    public void setCandidateHasFullName(boolean candidateHasFullName) {
        this.candidateHasFullName = candidateHasFullName;
    }
}

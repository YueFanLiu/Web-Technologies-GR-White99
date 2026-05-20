package fr.isep.projectweb.model.dto.response;

public class AccessibilityPreferencesResponse {

    private Boolean wheelchairAccessible;
    private Boolean elevatorNeeded;
    private Boolean accessibleRestroom;
    private Boolean quietEnvironment;
    private Boolean stepFreeAccess;

    public Boolean getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(Boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public Boolean getElevatorNeeded() {
        return elevatorNeeded;
    }

    public void setElevatorNeeded(Boolean elevatorNeeded) {
        this.elevatorNeeded = elevatorNeeded;
    }

    public Boolean getAccessibleRestroom() {
        return accessibleRestroom;
    }

    public void setAccessibleRestroom(Boolean accessibleRestroom) {
        this.accessibleRestroom = accessibleRestroom;
    }

    public Boolean getQuietEnvironment() {
        return quietEnvironment;
    }

    public void setQuietEnvironment(Boolean quietEnvironment) {
        this.quietEnvironment = quietEnvironment;
    }

    public Boolean getStepFreeAccess() {
        return stepFreeAccess;
    }

    public void setStepFreeAccess(Boolean stepFreeAccess) {
        this.stepFreeAccess = stepFreeAccess;
    }
}

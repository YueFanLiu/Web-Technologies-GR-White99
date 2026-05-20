package fr.isep.projectweb.model.dto.request;

public class UpdateMyProfileRequest {

    private String fullName;
    private String phone;
    private AccessibilityPreferencesRequest accessibilityPreferences;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public AccessibilityPreferencesRequest getAccessibilityPreferences() {
        return accessibilityPreferences;
    }

    public void setAccessibilityPreferences(AccessibilityPreferencesRequest accessibilityPreferences) {
        this.accessibilityPreferences = accessibilityPreferences;
    }
}

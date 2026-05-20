package fr.isep.projectweb.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_accessibility_preferences")
public class UserAccessibilityPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "wheelchair_accessible", nullable = false)
    private Boolean wheelchairAccessible = false;

    @Column(name = "elevator_needed", nullable = false)
    private Boolean elevatorNeeded = false;

    @Column(name = "accessible_restroom", nullable = false)
    private Boolean accessibleRestroom = false;

    @Column(name = "quiet_environment", nullable = false)
    private Boolean quietEnvironment = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

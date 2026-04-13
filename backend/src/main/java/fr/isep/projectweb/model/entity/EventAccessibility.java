package fr.isep.projectweb.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "event_accessibility")
public class EventAccessibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "wheelchair_accessible")
    private Boolean wheelchairAccessible;

    @Column(name = "has_elevator")
    private Boolean hasElevator;

    @Column(name = "accessible_toilet")
    private Boolean accessibleToilet;

    @Column(name = "quiet_environment")
    private Boolean quietEnvironment;

    @Column(name = "step_free_access")
    private Boolean stepFreeAccess;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public EventAccessibility() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Boolean getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(Boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public Boolean getHasElevator() {
        return hasElevator;
    }

    public void setHasElevator(Boolean hasElevator) {
        this.hasElevator = hasElevator;
    }

    public Boolean getAccessibleToilet() {
        return accessibleToilet;
    }

    public void setAccessibleToilet(Boolean accessibleToilet) {
        this.accessibleToilet = accessibleToilet;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

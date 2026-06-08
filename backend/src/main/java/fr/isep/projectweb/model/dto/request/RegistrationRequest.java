package fr.isep.projectweb.model.dto.request;

import java.util.UUID;

public class RegistrationRequest {

    private UUID eventId;
    private String status;
    private String contactFullName;
    private String contactEmail;
    private String contactPhone;
    private UUID ticketTierId;
    private Integer quantity;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContactFullName() {
        return contactFullName;
    }

    public void setContactFullName(String contactFullName) {
        this.contactFullName = contactFullName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public UUID getTicketTierId() {
        return ticketTierId;
    }

    public void setTicketTierId(UUID ticketTierId) {
        this.ticketTierId = ticketTierId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

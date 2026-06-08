package fr.isep.projectweb.model.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

public class RegistrationResponse {

    private UUID id;
    private EventSummaryResponse event;
    private UserSummaryResponse user;
    private String status;
    private LocalDateTime registeredAt;
    private String contactFullName;
    private String contactEmail;
    private String contactPhone;
    private UUID ticketTierId;
    private String ticketTierName;
    private String ticketTierType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String currency;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EventSummaryResponse getEvent() {
        return event;
    }

    public void setEvent(EventSummaryResponse event) {
        this.event = event;
    }

    public UserSummaryResponse getUser() {
        return user;
    }

    public void setUser(UserSummaryResponse user) {
        this.user = user;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
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

    public String getTicketTierName() {
        return ticketTierName;
    }

    public void setTicketTierName(String ticketTierName) {
        this.ticketTierName = ticketTierName;
    }

    public String getTicketTierType() {
        return ticketTierType;
    }

    public void setTicketTierType(String ticketTierType) {
        this.ticketTierType = ticketTierType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

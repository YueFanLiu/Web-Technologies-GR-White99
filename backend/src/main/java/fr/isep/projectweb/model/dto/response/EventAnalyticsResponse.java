package fr.isep.projectweb.model.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class EventAnalyticsResponse {

    private UUID eventId;
    private Integer capacity;
    private Long totalRegistrations;
    private Long registeredCount;
    private Long confirmedCount;
    private Long cancelledCount;
    private Long soldQuantity;
    private Long remainingSpots;
    private BigDecimal ticketSalesTotal;
    private Double averageRating;
    private Long reviewCount;
    private List<TicketTierAnalyticsResponse> ticketTiers;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(Long totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public Long getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(Long registeredCount) {
        this.registeredCount = registeredCount;
    }

    public Long getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(Long confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public Long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(Long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public Long getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(Long soldQuantity) {
        this.soldQuantity = soldQuantity;
    }

    public Long getRemainingSpots() {
        return remainingSpots;
    }

    public void setRemainingSpots(Long remainingSpots) {
        this.remainingSpots = remainingSpots;
    }

    public BigDecimal getTicketSalesTotal() {
        return ticketSalesTotal;
    }

    public void setTicketSalesTotal(BigDecimal ticketSalesTotal) {
        this.ticketSalesTotal = ticketSalesTotal;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public List<TicketTierAnalyticsResponse> getTicketTiers() {
        return ticketTiers;
    }

    public void setTicketTiers(List<TicketTierAnalyticsResponse> ticketTiers) {
        this.ticketTiers = ticketTiers;
    }
}

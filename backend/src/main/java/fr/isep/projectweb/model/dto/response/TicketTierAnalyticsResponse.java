package fr.isep.projectweb.model.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public class TicketTierAnalyticsResponse {

    private UUID tierId;
    private String name;
    private String type;
    private BigDecimal price;
    private Integer capacity;
    private Long soldQuantity;
    private Long remainingQuantity;
    private BigDecimal revenue;

    public UUID getTierId() {
        return tierId;
    }

    public void setTierId(UUID tierId) {
        this.tierId = tierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(Long soldQuantity) {
        this.soldQuantity = soldQuantity;
    }

    public Long getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Long remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}

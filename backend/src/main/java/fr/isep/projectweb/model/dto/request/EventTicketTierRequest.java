package fr.isep.projectweb.model.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventTicketTierRequest {

    private String name;
    private String type;
    private BigDecimal price;
    private Integer capacity;
    private LocalDateTime salesStartAt;
    private LocalDateTime salesEndAt;
    private Boolean active;
    private Integer sortOrder;

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

    public LocalDateTime getSalesStartAt() {
        return salesStartAt;
    }

    public void setSalesStartAt(LocalDateTime salesStartAt) {
        this.salesStartAt = salesStartAt;
    }

    public LocalDateTime getSalesEndAt() {
        return salesEndAt;
    }

    public void setSalesEndAt(LocalDateTime salesEndAt) {
        this.salesEndAt = salesEndAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}

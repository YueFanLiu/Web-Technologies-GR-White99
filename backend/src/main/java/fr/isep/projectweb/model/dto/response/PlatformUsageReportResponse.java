package fr.isep.projectweb.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PlatformUsageReportResponse {

    private LocalDate from;
    private LocalDate to;
    private Long totalUsers;
    private Long totalEvents;
    private Long totalPosts;
    private Long totalRegistrations;
    private BigDecimal ticketSalesTotal;
    private Double averageFeedbackRating;
    private List<MetricCountResponse> usersByRole;
    private List<MetricCountResponse> usersByStatus;
    private List<TopEventMetricResponse> topEventsByRegistrations;
    private List<TopEventMetricResponse> topEventsByRevenue;
    private List<TopEventMetricResponse> topEventsByRating;

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(Long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public Long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Long totalPosts) {
        this.totalPosts = totalPosts;
    }

    public Long getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(Long totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public BigDecimal getTicketSalesTotal() {
        return ticketSalesTotal;
    }

    public void setTicketSalesTotal(BigDecimal ticketSalesTotal) {
        this.ticketSalesTotal = ticketSalesTotal;
    }

    public Double getAverageFeedbackRating() {
        return averageFeedbackRating;
    }

    public void setAverageFeedbackRating(Double averageFeedbackRating) {
        this.averageFeedbackRating = averageFeedbackRating;
    }

    public List<MetricCountResponse> getUsersByRole() {
        return usersByRole;
    }

    public void setUsersByRole(List<MetricCountResponse> usersByRole) {
        this.usersByRole = usersByRole;
    }

    public List<MetricCountResponse> getUsersByStatus() {
        return usersByStatus;
    }

    public void setUsersByStatus(List<MetricCountResponse> usersByStatus) {
        this.usersByStatus = usersByStatus;
    }

    public List<TopEventMetricResponse> getTopEventsByRegistrations() {
        return topEventsByRegistrations;
    }

    public void setTopEventsByRegistrations(List<TopEventMetricResponse> topEventsByRegistrations) {
        this.topEventsByRegistrations = topEventsByRegistrations;
    }

    public List<TopEventMetricResponse> getTopEventsByRevenue() {
        return topEventsByRevenue;
    }

    public void setTopEventsByRevenue(List<TopEventMetricResponse> topEventsByRevenue) {
        this.topEventsByRevenue = topEventsByRevenue;
    }

    public List<TopEventMetricResponse> getTopEventsByRating() {
        return topEventsByRating;
    }

    public void setTopEventsByRating(List<TopEventMetricResponse> topEventsByRating) {
        this.topEventsByRating = topEventsByRating;
    }

    public static class MetricCountResponse {
        private String label;
        private Long count;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

    public static class TopEventMetricResponse {
        private UUID eventId;
        private String title;
        private Long count;
        private BigDecimal revenue;
        private Double rating;

        public UUID getEventId() {
            return eventId;
        }

        public void setEventId(UUID eventId) {
            this.eventId = eventId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        public Double getRating() {
            return rating;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }
    }
}

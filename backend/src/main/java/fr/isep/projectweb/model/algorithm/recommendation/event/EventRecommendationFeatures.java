package fr.isep.projectweb.model.algorithm.recommendation.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * Event recommendation algorithm input features.
 *
 * This class follows the same responsibility split as the location
 * recommendation classes:
 *
 * 1. EventService queries event candidates for GET /api/events.
 * 2. EventService collects all signals needed by the algorithm.
 * 3. Those plain values are stored in EventRecommendationFeatures.
 * 4. EventRecommendationScorer computes a numeric score from these values.
 * 5. EventService sorts by that score and maps the final events to responses.
 *
 * The feature object intentionally does not depend on JPA entities,
 * repositories, DTOs, HTTP requests, or Spring. This keeps the scoring
 * algorithm easy to understand, test, and evolve without changing API
 * contracts.
 *
 * Current signal groups:
 *
 * 1. Keyword relevance:
 *    keyword against title, description, category, location name, and city.
 *
 * 2. Time value:
 *    upcoming events are more useful on the main page, and events happening
 *    soon receive a stronger boost than events far in the future.
 *
 * 3. Quality and popularity:
 *    average rating and review count reward events that users have interacted
 *    with positively.
 *
 * 4. Media:
 *    events with images are easier to evaluate in the frontend, so they get a
 *    small boost.
 *
 * 5. Status and completeness:
 *    published/active events are favored, cancelled/draft events are penalized,
 *    and complete event profiles receive small boosts.
 */
public class EventRecommendationFeatures {

    /*
     * Search context.
     *
     * keyword can be null because GET /api/events also supports browsing the
     * main activity page without a text query. In that case, keyword relevance
     * contributes zero points and the ranking is driven by time, quality, media,
     * status, and completeness.
     */
    private String keyword;

    /*
     * Searchable event text.
     *
     * These fields are copied from Event instead of passing the entity directly
     * so the scorer remains independent from persistence concerns.
     */
    private String title;
    private String description;
    private String category;

    /*
     * Location context.
     *
     * Location values let an event match searches like "Paris" or a venue name
     * even when the event title itself does not contain that word.
     */
    private String locationName;
    private String locationCity;
    private boolean hasLocation;

    /*
     * Time context.
     *
     * now is provided by the service so the scorer does not need to know where
     * the current time came from. This also makes future tests deterministic.
     */
    private LocalDateTime now;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /*
     * Event profile fields used for status and completeness scoring.
     */
    private String status;
    private Integer capacity;
    private BigDecimal price;
    private Boolean virtualEvent;

    /*
     * Quality, popularity, and media signals.
     *
     * averageRating can be null when no review exists. The scorer treats null
     * as no rating signal instead of assuming a bad rating.
     */
    private Double averageRating;
    private long reviewCount;
    private long imageCount;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public boolean isHasLocation() {
        return hasLocation;
    }

    public void setHasLocation(boolean hasLocation) {
        this.hasLocation = hasLocation;
    }

    public LocalDateTime getNow() {
        return now;
    }

    public void setNow(LocalDateTime now) {
        this.now = now;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getVirtualEvent() {
        return virtualEvent;
    }

    public void setVirtualEvent(Boolean virtualEvent) {
        this.virtualEvent = virtualEvent;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public long getImageCount() {
        return imageCount;
    }

    public void setImageCount(long imageCount) {
        this.imageCount = imageCount;
    }
}

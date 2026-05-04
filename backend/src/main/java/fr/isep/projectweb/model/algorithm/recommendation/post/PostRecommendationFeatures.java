package fr.isep.projectweb.model.algorithm.recommendation.post;

import java.time.LocalDateTime;

/*
 * Post recommendation algorithm input features.
 *
 * This class follows the same design as the location and event recommendation
 * feature objects:
 *
 * 1. PostService queries candidate posts for GET /api/posts.
 * 2. PostService collects all signals needed by the algorithm.
 * 3. Those plain values are stored in PostRecommendationFeatures.
 * 4. PostRecommendationScorer computes a numeric score from these values.
 * 5. PostService sorts by score and maps the final posts to responses.
 *
 * The feature object intentionally contains only plain values. It does not know
 * how to query the database, map DTOs, or handle HTTP requests. That keeps the
 * algorithm layer independent from JPA, Spring MVC, and response formatting.
 *
 * Current signal groups:
 *
 * 1. Keyword relevance:
 *    keyword against post title/content plus related location and event fields.
 *
 * 2. Freshness:
 *    recent posts are usually more useful in a community feed, so createdAt is
 *    used as a time-decay signal.
 *
 * 3. Quality and popularity:
 *    average rating and review count reward posts that have received useful
 *    user feedback.
 *
 * 4. Media:
 *    posts with images are easier to inspect and usually more engaging in the
 *    frontend feed.
 *
 * 5. Context and completeness:
 *    posts connected to a location or event get a small boost because they are
 *    easier to navigate and understand. Complete title/content/status values
 *    also help the ranking.
 */
public class PostRecommendationFeatures {

    /*
     * Search context.
     *
     * keyword is optional because GET /api/posts can be used as a general feed
     * without a text search. When it is null or blank, keyword relevance adds
     * zero points.
     */
    private String keyword;

    /*
     * Post text fields.
     */
    private String title;
    private String content;
    private String status;

    /*
     * Related location context.
     *
     * These values let searches such as "Paris" or a venue name match posts
     * that are attached to the relevant location.
     */
    private String locationName;
    private String locationCity;
    private boolean hasLocation;

    /*
     * Related event context.
     *
     * These values let event-related posts inherit useful discovery signals from
     * the event they discuss.
     */
    private String eventTitle;
    private String eventCategory;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    private boolean hasEvent;

    /*
     * Feed freshness.
     *
     * now is passed in by PostService so this class stays deterministic and the
     * scorer does not need to fetch the current time by itself.
     */
    private LocalDateTime now;
    private LocalDateTime createdAt;

    /*
     * Quality, popularity, and media signals.
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public LocalDateTime getEventStartTime() {
        return eventStartTime;
    }

    public void setEventStartTime(LocalDateTime eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    public LocalDateTime getEventEndTime() {
        return eventEndTime;
    }

    public void setEventEndTime(LocalDateTime eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    public boolean isHasEvent() {
        return hasEvent;
    }

    public void setHasEvent(boolean hasEvent) {
        this.hasEvent = hasEvent;
    }

    public LocalDateTime getNow() {
        return now;
    }

    public void setNow(LocalDateTime now) {
        this.now = now;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

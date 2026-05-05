package fr.isep.projectweb.model.algorithm.recommendation.event;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/*
 * Event recommendation/ranking scorer.
 *
 * This class is the event equivalent of LocationRecommendationScorer. It is
 * designed for GET /api/events:
 *
 * 1. EventService fetches a candidate list using the request filters.
 * 2. EventService builds EventRecommendationFeatures for each candidate.
 * 3. This scorer computes one score per event.
 * 4. EventService sorts by score descending and returns the final page.
 *
 * The scorer does not query repositories, does not know DTOs, and does not
 * know HTTP parameters. Its only job is to convert feature values into a score.
 * Higher score means the event should appear earlier in the main activity page.
 *
 * Current score parts:
 *
 * 1. Keyword relevance:
 *    Strong title matches are the most important search signal. Category and
 *    location matches are also useful. Description matches are weaker because
 *    descriptions can contain broad text.
 *
 * 2. Time value:
 *    Upcoming events are more actionable than distant or past events. Events
 *    starting soon get the largest boost. Past events are not aggressively
 *    penalized here because upcomingOnly can already filter them out, but they
 *    receive no time boost.
 *
 * 3. Quality:
 *    A high average rating and a reasonable number of reviews indicate that
 *    users have found the event useful. Both components are capped so a single
 *    very popular event does not dominate every result.
 *
 * 4. Media:
 *    At least one image gives a meaningful boost. Additional images give only a
 *    small capped boost.
 *
 * 5. Status and completeness:
 *    Published/active events are favored. Draft and cancelled events are pushed
 *    down. Complete event records get small boosts because they are more useful
 *    to the frontend and to users.
 */
@Component
public class EventRecommendationScorer {

    /*
     * Keyword weights.
     *
     * Title is weighted highest because users often search for event names.
     * Category and location are secondary discovery signals. Description is
     * broad, so it receives the smallest keyword weight.
     */
    private static final double EXACT_TITLE_MATCH_WEIGHT = 30.0;
    private static final double TITLE_MATCH_WEIGHT = 20.0;
    private static final double CATEGORY_MATCH_WEIGHT = 12.0;
    private static final double LOCATION_NAME_MATCH_WEIGHT = 8.0;
    private static final double LOCATION_CITY_MATCH_WEIGHT = 8.0;
    private static final double DESCRIPTION_MATCH_WEIGHT = 4.0;

    /*
     * Time weights.
     *
     * The windows are intentionally simple:
     * - happening now or within 7 days: very relevant
     * - within 30 days: still useful
     * - later future: valid but less urgent
     */
    private static final double HAPPENING_NOW_WEIGHT = 22.0;
    private static final double STARTS_WITHIN_7_DAYS_WEIGHT = 20.0;
    private static final double STARTS_WITHIN_30_DAYS_WEIGHT = 12.0;
    private static final double FUTURE_EVENT_WEIGHT = 5.0;

    /*
     * Quality weights.
     *
     * averageRating is expected to be in the usual 1..5 range. We multiply by 2
     * and cap at 10. Review count adds popularity but is capped separately.
     */
    private static final double RATING_WEIGHT = 2.0;
    private static final double MAX_RATING_SCORE = 10.0;
    private static final double REVIEW_COUNT_WEIGHT = 1.0;
    private static final double MAX_REVIEW_COUNT_SCORE = 10.0;

    /*
     * Media weights.
     *
     * One image matters most because it provides a cover/preview. Extra images
     * are useful but should not overpower relevance or time.
     */
    private static final double HAS_IMAGE_WEIGHT = 3.0;
    private static final double EXTRA_IMAGE_WEIGHT = 0.5;
    private static final long MAX_EXTRA_IMAGE_COUNT = 4;

    /*
     * Status weights.
     *
     * DRAFT and CANCELLED are not hidden by the scorer because filtering is the
     * repository/service responsibility. The scorer only pushes them down if
     * they appear in a candidate set.
     */
    private static final double PUBLISHED_STATUS_WEIGHT = 5.0;
    private static final double ACTIVE_STATUS_WEIGHT = 5.0;
    private static final double DRAFT_STATUS_PENALTY = -10.0;
    private static final double CANCELLED_STATUS_PENALTY = -30.0;

    /*
     * Completeness weights.
     *
     * These are intentionally small. They help break ties between otherwise
     * similar events without defeating keyword relevance, time, or quality.
     */
    private static final double HAS_DESCRIPTION_WEIGHT = 2.0;
    private static final double HAS_CATEGORY_WEIGHT = 1.0;
    private static final double HAS_LOCATION_WEIGHT = 2.0;
    private static final double HAS_CAPACITY_WEIGHT = 1.0;
    private static final double HAS_PRICE_WEIGHT = 1.0;
    private static final double FREE_EVENT_WEIGHT = 2.0;
    private static final double VIRTUAL_EVENT_WEIGHT = 1.0;

    /**
     * Computes the final recommendation score.
     *
     * @param features all precomputed signals for one event candidate
     * @return score used for descending sort order
     */
    public double score(EventRecommendationFeatures features) {
        if (features == null) {
            return 0.0;
        }

        return keywordRelevanceScore(features)
                + timeScore(features)
                + qualityScore(features)
                + imageScore(features)
                + statusScore(features)
                + completenessScore(features);
    }

    private double keywordRelevanceScore(EventRecommendationFeatures features) {
        String keyword = normalize(features.getKeyword());
        if (keyword == null) {
            return 0.0;
        }

        double score = 0.0;
        String title = normalize(features.getTitle());

        if (title != null && title.equals(keyword)) {
            score += EXACT_TITLE_MATCH_WEIGHT;
        } else if (contains(features.getTitle(), keyword)) {
            score += TITLE_MATCH_WEIGHT;
        }

        if (contains(features.getCategory(), keyword)) {
            score += CATEGORY_MATCH_WEIGHT;
        }
        if (contains(features.getLocationName(), keyword)) {
            score += LOCATION_NAME_MATCH_WEIGHT;
        }
        if (contains(features.getLocationCity(), keyword)) {
            score += LOCATION_CITY_MATCH_WEIGHT;
        }
        if (contains(features.getDescription(), keyword)) {
            score += DESCRIPTION_MATCH_WEIGHT;
        }

        return score;
    }

    private double timeScore(EventRecommendationFeatures features) {
        LocalDateTime now = features.getNow();
        LocalDateTime startTime = features.getStartTime();
        LocalDateTime endTime = features.getEndTime();

        if (now == null || startTime == null) {
            return 0.0;
        }

        if (endTime != null && !now.isBefore(startTime) && !now.isAfter(endTime)) {
            return HAPPENING_NOW_WEIGHT;
        }
        if (startTime.isBefore(now)) {
            return 0.0;
        }

        long daysUntilStart = Duration.between(now, startTime).toDays();
        if (daysUntilStart <= 7) {
            return STARTS_WITHIN_7_DAYS_WEIGHT;
        }
        if (daysUntilStart <= 30) {
            return STARTS_WITHIN_30_DAYS_WEIGHT;
        }
        return FUTURE_EVENT_WEIGHT;
    }

    private double qualityScore(EventRecommendationFeatures features) {
        double ratingScore = 0.0;
        Double averageRating = features.getAverageRating();
        if (averageRating != null && averageRating > 0) {
            ratingScore = Math.min(averageRating * RATING_WEIGHT, MAX_RATING_SCORE);
        }

        double reviewScore = cappedScore(features.getReviewCount(), REVIEW_COUNT_WEIGHT, MAX_REVIEW_COUNT_SCORE);
        return ratingScore + reviewScore;
    }

    private double imageScore(EventRecommendationFeatures features) {
        long imageCount = Math.max(features.getImageCount(), 0);
        if (imageCount == 0) {
            return 0.0;
        }

        long extraImageCount = Math.min(imageCount - 1, MAX_EXTRA_IMAGE_COUNT);
        return HAS_IMAGE_WEIGHT + extraImageCount * EXTRA_IMAGE_WEIGHT;
    }

    private double statusScore(EventRecommendationFeatures features) {
        String status = normalize(features.getStatus());
        if (status == null) {
            return 0.0;
        }

        return switch (status) {
            case "published" -> PUBLISHED_STATUS_WEIGHT;
            case "active" -> ACTIVE_STATUS_WEIGHT;
            case "draft" -> DRAFT_STATUS_PENALTY;
            case "cancelled", "canceled" -> CANCELLED_STATUS_PENALTY;
            default -> 0.0;
        };
    }

    private double completenessScore(EventRecommendationFeatures features) {
        double score = 0.0;

        if (hasText(features.getDescription())) {
            score += HAS_DESCRIPTION_WEIGHT;
        }
        if (hasText(features.getCategory())) {
            score += HAS_CATEGORY_WEIGHT;
        }
        if (features.isHasLocation()) {
            score += HAS_LOCATION_WEIGHT;
        }
        if (features.getCapacity() != null && features.getCapacity() > 0) {
            score += HAS_CAPACITY_WEIGHT;
        }
        if (features.getPrice() != null) {
            score += HAS_PRICE_WEIGHT;
            if (BigDecimal.ZERO.compareTo(features.getPrice()) == 0) {
                score += FREE_EVENT_WEIGHT;
            }
        }
        if (Boolean.TRUE.equals(features.getVirtualEvent())) {
            score += VIRTUAL_EVENT_WEIGHT;
        }

        return score;
    }

    private double cappedScore(long count, double weight, double maxScore) {
        return Math.min(Math.max(count, 0) * weight, maxScore);
    }

    private boolean contains(String value, String normalizedKeyword) {
        String normalizedValue = normalize(value);
        return normalizedValue != null && normalizedValue.contains(normalizedKeyword);
    }

    private boolean hasText(String value) {
        return normalize(value) != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}

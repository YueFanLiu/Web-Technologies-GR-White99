package fr.isep.projectweb.model.algorithm.recommendation.post;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/*
 * Post recommendation/ranking scorer.
 *
 * This class is the post equivalent of LocationRecommendationScorer and
 * EventRecommendationScorer. It is designed for GET /api/posts:
 *
 * 1. PostService fetches a candidate list using optional feed filters.
 * 2. PostService builds PostRecommendationFeatures for each candidate.
 * 3. This scorer computes one score per post.
 * 4. PostService sorts by score descending and returns the final feed.
 *
 * The scorer is intentionally pure: it does not query repositories, does not
 * return DTOs, and does not know controller parameters. It only turns feature
 * values into a numeric ranking score.
 *
 * Current score parts:
 *
 * 1. Keyword relevance:
 *    Post title is strongest, post content is secondary, and related event or
 *    location fields can also match the search intent.
 *
 * 2. Freshness:
 *    New posts are favored for the main community feed. Older posts can still
 *    rank well if they are strongly relevant or highly rated.
 *
 * 3. Quality:
 *    Average rating and review count reward useful posts, with caps to prevent
 *    one highly reviewed post from dominating every result.
 *
 * 4. Media:
 *    At least one image gives a visible feed-quality boost. Extra images add a
 *    small capped boost.
 *
 * 5. Context, status, and completeness:
 *    Posts linked to a location/event are easier to navigate. Published/active
 *    posts are favored, while draft/hidden/deleted posts are pushed down if they
 *    are included in the candidate set.
 */
@Component
public class PostRecommendationScorer {

    /*
     * Keyword weights.
     *
     * Title is the strongest post-level signal. Content can be long and broad,
     * so it receives less weight. Event and location fields are useful context
     * signals for discovery.
     */
    private static final double EXACT_TITLE_MATCH_WEIGHT = 30.0;
    private static final double TITLE_MATCH_WEIGHT = 20.0;
    private static final double EVENT_TITLE_MATCH_WEIGHT = 14.0;
    private static final double EVENT_CATEGORY_MATCH_WEIGHT = 10.0;
    private static final double LOCATION_NAME_MATCH_WEIGHT = 8.0;
    private static final double LOCATION_CITY_MATCH_WEIGHT = 8.0;
    private static final double CONTENT_MATCH_WEIGHT = 5.0;

    /*
     * Freshness weights.
     *
     * A community feed usually benefits from recent content. These windows keep
     * the model explainable and avoid hard-to-debug exponential decay.
     */
    private static final double CREATED_WITHIN_1_DAY_WEIGHT = 18.0;
    private static final double CREATED_WITHIN_7_DAYS_WEIGHT = 14.0;
    private static final double CREATED_WITHIN_30_DAYS_WEIGHT = 8.0;
    private static final double OLDER_POST_WEIGHT = 2.0;

    /*
     * Event timing context.
     *
     * A post connected to an upcoming or currently running event gets a small
     * boost because it may help users decide whether to attend.
     */
    private static final double RELATED_EVENT_HAPPENING_NOW_WEIGHT = 5.0;
    private static final double RELATED_EVENT_UPCOMING_WEIGHT = 4.0;

    /*
     * Quality weights.
     */
    private static final double RATING_WEIGHT = 2.0;
    private static final double MAX_RATING_SCORE = 10.0;
    private static final double REVIEW_COUNT_WEIGHT = 1.0;
    private static final double MAX_REVIEW_COUNT_SCORE = 10.0;

    /*
     * Media weights.
     */
    private static final double HAS_IMAGE_WEIGHT = 4.0;
    private static final double EXTRA_IMAGE_WEIGHT = 0.5;
    private static final long MAX_EXTRA_IMAGE_COUNT = 4;

    /*
     * Status weights.
     */
    private static final double PUBLISHED_STATUS_WEIGHT = 5.0;
    private static final double ACTIVE_STATUS_WEIGHT = 5.0;
    private static final double DRAFT_STATUS_PENALTY = -10.0;
    private static final double HIDDEN_STATUS_PENALTY = -20.0;
    private static final double DELETED_STATUS_PENALTY = -30.0;

    /*
     * Context and completeness weights.
     */
    private static final double HAS_LOCATION_WEIGHT = 2.0;
    private static final double HAS_EVENT_WEIGHT = 2.0;
    private static final double HAS_CONTENT_WEIGHT = 2.0;
    private static final double LONG_CONTENT_WEIGHT = 1.0;

    /**
     * Computes the final recommendation score.
     *
     * @param features all precomputed signals for one post candidate
     * @return score used for descending sort order
     */
    public double score(PostRecommendationFeatures features) {
        if (features == null) {
            return 0.0;
        }

        return keywordRelevanceScore(features)
                + freshnessScore(features)
                + relatedEventTimeScore(features)
                + qualityScore(features)
                + imageScore(features)
                + statusScore(features)
                + completenessScore(features);
    }

    private double keywordRelevanceScore(PostRecommendationFeatures features) {
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

        if (contains(features.getEventTitle(), keyword)) {
            score += EVENT_TITLE_MATCH_WEIGHT;
        }
        if (contains(features.getEventCategory(), keyword)) {
            score += EVENT_CATEGORY_MATCH_WEIGHT;
        }
        if (contains(features.getLocationName(), keyword)) {
            score += LOCATION_NAME_MATCH_WEIGHT;
        }
        if (contains(features.getLocationCity(), keyword)) {
            score += LOCATION_CITY_MATCH_WEIGHT;
        }
        if (contains(features.getContent(), keyword)) {
            score += CONTENT_MATCH_WEIGHT;
        }

        return score;
    }

    private double freshnessScore(PostRecommendationFeatures features) {
        LocalDateTime now = features.getNow();
        LocalDateTime createdAt = features.getCreatedAt();
        if (now == null || createdAt == null || createdAt.isAfter(now)) {
            return 0.0;
        }

        long daysOld = Duration.between(createdAt, now).toDays();
        if (daysOld <= 1) {
            return CREATED_WITHIN_1_DAY_WEIGHT;
        }
        if (daysOld <= 7) {
            return CREATED_WITHIN_7_DAYS_WEIGHT;
        }
        if (daysOld <= 30) {
            return CREATED_WITHIN_30_DAYS_WEIGHT;
        }
        return OLDER_POST_WEIGHT;
    }

    private double relatedEventTimeScore(PostRecommendationFeatures features) {
        if (!features.isHasEvent()) {
            return 0.0;
        }

        LocalDateTime now = features.getNow();
        LocalDateTime startTime = features.getEventStartTime();
        LocalDateTime endTime = features.getEventEndTime();
        if (now == null || startTime == null) {
            return 0.0;
        }

        if (endTime != null && !now.isBefore(startTime) && !now.isAfter(endTime)) {
            return RELATED_EVENT_HAPPENING_NOW_WEIGHT;
        }
        if (startTime.isAfter(now)) {
            return RELATED_EVENT_UPCOMING_WEIGHT;
        }
        return 0.0;
    }

    private double qualityScore(PostRecommendationFeatures features) {
        double ratingScore = 0.0;
        Double averageRating = features.getAverageRating();
        if (averageRating != null && averageRating > 0) {
            ratingScore = Math.min(averageRating * RATING_WEIGHT, MAX_RATING_SCORE);
        }

        double reviewScore = cappedScore(features.getReviewCount(), REVIEW_COUNT_WEIGHT, MAX_REVIEW_COUNT_SCORE);
        return ratingScore + reviewScore;
    }

    private double imageScore(PostRecommendationFeatures features) {
        long imageCount = Math.max(features.getImageCount(), 0);
        if (imageCount == 0) {
            return 0.0;
        }

        long extraImageCount = Math.min(imageCount - 1, MAX_EXTRA_IMAGE_COUNT);
        return HAS_IMAGE_WEIGHT + extraImageCount * EXTRA_IMAGE_WEIGHT;
    }

    private double statusScore(PostRecommendationFeatures features) {
        String status = normalize(features.getStatus());
        if (status == null) {
            return 0.0;
        }

        return switch (status) {
            case "published" -> PUBLISHED_STATUS_WEIGHT;
            case "active" -> ACTIVE_STATUS_WEIGHT;
            case "draft" -> DRAFT_STATUS_PENALTY;
            case "hidden" -> HIDDEN_STATUS_PENALTY;
            case "deleted" -> DELETED_STATUS_PENALTY;
            default -> 0.0;
        };
    }

    private double completenessScore(PostRecommendationFeatures features) {
        double score = 0.0;

        if (features.isHasLocation()) {
            score += HAS_LOCATION_WEIGHT;
        }
        if (features.isHasEvent()) {
            score += HAS_EVENT_WEIGHT;
        }
        if (hasText(features.getContent())) {
            score += HAS_CONTENT_WEIGHT;
            if (features.getContent().trim().length() >= 120) {
                score += LONG_CONTENT_WEIGHT;
            }
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

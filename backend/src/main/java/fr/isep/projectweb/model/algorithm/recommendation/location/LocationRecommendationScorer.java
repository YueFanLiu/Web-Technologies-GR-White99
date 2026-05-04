package fr.isep.projectweb.model.algorithm.recommendation.location;

import org.springframework.stereotype.Component;

import java.util.Locale;

/*
 * Location 推荐/排序算法说明：
 *
 * 这个类是 location 推荐算法的“打分器”，目前已经接入到：
 * GET /api/locations/search?keyword=xxx
 *
 * 接口调用流程如下：
 * 1. LocationService.searchLocations 先根据 keyword 从 locations 表中查一批候选地点。
 * 2. LocationService 再从 events、posts、location_images、location_accessibility 等表中
 *    收集每个候选地点的辅助信息。
 * 3. 这些数据会被组装成 LocationRecommendationFeatures。
 * 4. 本类只负责根据 features 计算 score。
 * 5. LocationService 按 score 倒序排序后，返回前 20 个 LocationResponse。
 *
 * 也就是说：
 * - Controller 不知道具体算法细节，只调用 LocationService。
 * - Service 负责查数据库、组装特征、调用算法、排序。
 * - Scorer 不查数据库、不调用 DAO、不返回 DTO，只负责计算分数。
 *
 * score 越高，表示该 location 在搜索结果中越应该排在前面。
 *
 * 当前总分由五部分组成：
 *
 * 1. 关键词相关性 keywordRelevanceScore
 *    - name 完全等于 keyword：+30
 *    - name 包含 keyword：+20
 *    - city 包含 keyword：+12
 *    - country 包含 keyword：+8
 *    - address 包含 keyword：+6
 *    - description 包含 keyword：+4
 *
 * 2. 活跃度 activityScore
 *    - upcomingEventCount * 5，最高 25 分
 *    - eventCount * 2，最高 20 分
 *    - postCount * 1.5，最高 15 分
 *    这里给 upcoming event 更高权重，因为即将发生的活动比历史活动更有即时价值。
 *
 * 3. 无障碍 accessibilityScore
 *    - 每个 true 的无障碍字段 +2 分
 *    - 当前五个字段权重相同，后续如果产品需要，可以单独调高 wheelchair 等字段。
 *
 * 4. 图片 imageScore
 *    - 至少有一张图片：+3
 *    - 额外图片每张 +0.5，最多计算 3 张额外图片
 *    这样避免图片很多的地点过度压制其他地点。
 *
 * 5. 信息完整度 completenessScore
 *    - description 不为空：+2
 *    - address 不为空：+2
 *    - city 不为空：+1
 *    - country 不为空：+1
 *    - 同时有 latitude 和 longitude：+2
 *
 * 注意：
 * - 本类是 Spring Bean，由 LocationService 注入使用。
 * - 如果两个 location 分数一样，最终不是在这里处理，而是在 LocationService 中继续按 name、id 排序。
 * - 后续如果要做 post/event 推荐，可以复用同样分层：Features 保存输入，Scorer 计算分数，Service 负责接 API。
 */

/**
 * Pure scoring algorithm for ranking location candidates.
 *
 * The scorer is used by LocationService.searchLocations. It does not fetch data
 * and does not return DTOs. It only receives a feature object and returns a
 * numeric score. Higher score means the location should appear earlier in
 * recommendation/search results.
 *
 * The current scoring model is intentionally simple and explainable:
 *
 * 1. Keyword relevance: locations that match the user's search more strongly
 *    are ranked higher.
 * 2. Activity: locations with events, upcoming events, and posts are likely to
 *    be useful and active.
 * 3. Accessibility: locations with more accessibility information marked true
 *    get a boost.
 * 4. Media: locations with images get a small boost.
 * 5. Completeness: locations with richer profile data get a small boost.
 *
 * The score is not meant to be an absolute value. It is only useful for sorting
 * candidates produced by a search or recommendation query.
 */
@Component
public class LocationRecommendationScorer {

    /*
     * Keyword weights.
     *
     * Name is the strongest signal because users often search for the exact
     * place name. City/country/address are still relevant, but weaker because
     * they can match many locations at once. Description is useful but broad, so
     * it gets the smallest keyword weight.
     */
    private static final double EXACT_NAME_MATCH_WEIGHT = 30.0;
    private static final double NAME_MATCH_WEIGHT = 20.0;
    private static final double CITY_MATCH_WEIGHT = 12.0;
    private static final double COUNTRY_MATCH_WEIGHT = 8.0;
    private static final double ADDRESS_MATCH_WEIGHT = 6.0;
    private static final double DESCRIPTION_MATCH_WEIGHT = 4.0;

    /*
     * Activity weights.
     *
     * Upcoming events matter more than historical/all events because they tell
     * users that the location has something actionable soon. We cap each activity
     * component so one very active location does not dominate every search.
     */
    private static final double UPCOMING_EVENT_WEIGHT = 5.0;
    private static final double EVENT_WEIGHT = 2.0;
    private static final double POST_WEIGHT = 1.5;
    private static final double MAX_UPCOMING_EVENT_SCORE = 25.0;
    private static final double MAX_EVENT_SCORE = 20.0;
    private static final double MAX_POST_SCORE = 15.0;

    /*
     * Accessibility weights.
     *
     * Every true accessibility flag contributes the same amount in the first
     * version. This keeps the result easy to explain. If the product later needs
     * to prioritize wheelchair access more heavily than quiet environment, these
     * constants can be split into separate weights.
     */
    private static final double ACCESSIBILITY_FLAG_WEIGHT = 2.0;

    /*
     * Media and completeness weights.
     *
     * One image gives a meaningful boost. Additional images give only a small
     * incremental boost because the difference between 0 and 1 image is usually
     * much more important than the difference between 5 and 6 images.
     */
    private static final double HAS_IMAGE_WEIGHT = 3.0;
    private static final double EXTRA_IMAGE_WEIGHT = 0.5;
    private static final int MAX_EXTRA_IMAGE_COUNT = 3;

    private static final double HAS_DESCRIPTION_WEIGHT = 2.0;
    private static final double HAS_ADDRESS_WEIGHT = 2.0;
    private static final double HAS_CITY_WEIGHT = 1.0;
    private static final double HAS_COUNTRY_WEIGHT = 1.0;
    private static final double HAS_COORDINATES_WEIGHT = 2.0;

    /**
     * Computes the final recommendation score.
     *
     * @param features all precomputed signals for one location candidate
     * @return score used for descending sort order
     */
    public double score(LocationRecommendationFeatures features) {
        if (features == null) {
            return 0.0;
        }

        return keywordRelevanceScore(features)
                + activityScore(features)
                + accessibilityScore(features)
                + imageScore(features)
                + completenessScore(features);
    }

    private double keywordRelevanceScore(LocationRecommendationFeatures features) {
        String keyword = normalize(features.getKeyword());
        if (keyword == null) {
            return 0.0;
        }

        double score = 0.0;
        String name = normalize(features.getName());

        if (name != null && name.equals(keyword)) {
            score += EXACT_NAME_MATCH_WEIGHT;
        } else if (contains(name, keyword)) {
            score += NAME_MATCH_WEIGHT;
        }

        if (contains(features.getCity(), keyword)) {
            score += CITY_MATCH_WEIGHT;
        }
        if (contains(features.getCountry(), keyword)) {
            score += COUNTRY_MATCH_WEIGHT;
        }
        if (contains(features.getAddress(), keyword)) {
            score += ADDRESS_MATCH_WEIGHT;
        }
        if (contains(features.getDescription(), keyword)) {
            score += DESCRIPTION_MATCH_WEIGHT;
        }

        return score;
    }

    private double activityScore(LocationRecommendationFeatures features) {
        double upcomingEventScore = cappedScore(
                features.getUpcomingEventCount(),
                UPCOMING_EVENT_WEIGHT,
                MAX_UPCOMING_EVENT_SCORE
        );
        double eventScore = cappedScore(features.getEventCount(), EVENT_WEIGHT, MAX_EVENT_SCORE);
        double postScore = cappedScore(features.getPostCount(), POST_WEIGHT, MAX_POST_SCORE);

        return upcomingEventScore + eventScore + postScore;
    }

    private double accessibilityScore(LocationRecommendationFeatures features) {
        int trueFlagCount = 0;
        trueFlagCount += asPoint(features.getWheelchairAccessible());
        trueFlagCount += asPoint(features.getHasElevator());
        trueFlagCount += asPoint(features.getAccessibleToilet());
        trueFlagCount += asPoint(features.getQuietEnvironment());
        trueFlagCount += asPoint(features.getStepFreeAccess());
        return trueFlagCount * ACCESSIBILITY_FLAG_WEIGHT;
    }

    private double imageScore(LocationRecommendationFeatures features) {
        int imageCount = Math.max(features.getImageCount(), 0);
        if (imageCount == 0) {
            return 0.0;
        }

        int extraImageCount = Math.min(imageCount - 1, MAX_EXTRA_IMAGE_COUNT);
        return HAS_IMAGE_WEIGHT + extraImageCount * EXTRA_IMAGE_WEIGHT;
    }

    private double completenessScore(LocationRecommendationFeatures features) {
        double score = 0.0;

        if (hasText(features.getDescription())) {
            score += HAS_DESCRIPTION_WEIGHT;
        }
        if (hasText(features.getAddress())) {
            score += HAS_ADDRESS_WEIGHT;
        }
        if (hasText(features.getCity())) {
            score += HAS_CITY_WEIGHT;
        }
        if (hasText(features.getCountry())) {
            score += HAS_COUNTRY_WEIGHT;
        }
        if (features.isHasCoordinates()) {
            score += HAS_COORDINATES_WEIGHT;
        }

        return score;
    }

    private double cappedScore(int count, double weight, double maxScore) {
        return Math.min(Math.max(count, 0) * weight, maxScore);
    }

    private int asPoint(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
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

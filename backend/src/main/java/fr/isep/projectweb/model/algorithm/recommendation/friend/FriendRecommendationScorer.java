package fr.isep.projectweb.model.algorithm.recommendation.friend;

import org.springframework.stereotype.Component;

import java.util.Locale;

/*
 * Friend recommendation scorer.
 *
 * The strongest signal is the social graph: friends-of-friends / mutual
 * friends are weighted much higher than weak activity overlaps. This matches
 * how users normally decide whom to add: a trusted common connection should be
 * more important than simply attending the same event once.
 *
 * Complexity per candidate is O(1) after the service precomputes set
 * intersections. Sorting N candidates is O(N log N).
 */
@Component
public class FriendRecommendationScorer {

    private static final double MUTUAL_FRIEND_WEIGHT = 18.0;
    private static final double MAX_MUTUAL_FRIEND_SCORE = 90.0;
    private static final double SHARED_EVENT_WEIGHT = 10.0;
    private static final double MAX_SHARED_EVENT_SCORE = 50.0;
    private static final double SHARED_SAVED_EVENT_WEIGHT = 5.0;
    private static final double MAX_SHARED_SAVED_EVENT_SCORE = 30.0;
    private static final double SHARED_REVIEW_TARGET_WEIGHT = 3.0;
    private static final double MAX_SHARED_REVIEW_TARGET_SCORE = 18.0;
    private static final double WHEELCHAIR_ACCESSIBLE_MATCH_WEIGHT = 8.0;
    private static final double ELEVATOR_MATCH_WEIGHT = 6.0;
    private static final double ACCESSIBLE_RESTROOM_MATCH_WEIGHT = 6.0;
    private static final double QUIET_ENVIRONMENT_MATCH_WEIGHT = 6.0;
    private static final double PARENT_PARENT_ROLE_WEIGHT = 10.0;
    private static final double SAME_ROLE_WEIGHT = 8.0;
    private static final double PARENT_ORGANIZER_ROLE_WEIGHT = 3.0;
    private static final double HAS_PHOTO_WEIGHT = 2.0;
    private static final double HAS_FULL_NAME_WEIGHT = 2.0;

    public double score(FriendRecommendationFeatures features) {
        if (features == null) {
            return 0.0;
        }

        return mutualFriendScore(features)
                + sharedActivityScore(features)
                + accessibilityScore(features)
                + roleScore(features)
                + profileCompletenessScore(features);
    }

    private double mutualFriendScore(FriendRecommendationFeatures features) {
        return capped(features.getMutualFriendCount(), MUTUAL_FRIEND_WEIGHT, MAX_MUTUAL_FRIEND_SCORE);
    }

    private double sharedActivityScore(FriendRecommendationFeatures features) {
        return capped(features.getSharedEventCount(), SHARED_EVENT_WEIGHT, MAX_SHARED_EVENT_SCORE)
                + capped(features.getSharedSavedEventCount(), SHARED_SAVED_EVENT_WEIGHT, MAX_SHARED_SAVED_EVENT_SCORE)
                + capped(features.getSharedReviewTargetCount(), SHARED_REVIEW_TARGET_WEIGHT, MAX_SHARED_REVIEW_TARGET_SCORE);
    }

    private double accessibilityScore(FriendRecommendationFeatures features) {
        double score = 0.0;
        if (features.isWheelchairAccessibleMatch()) {
            score += WHEELCHAIR_ACCESSIBLE_MATCH_WEIGHT;
        }
        if (features.isElevatorMatch()) {
            score += ELEVATOR_MATCH_WEIGHT;
        }
        if (features.isAccessibleRestroomMatch()) {
            score += ACCESSIBLE_RESTROOM_MATCH_WEIGHT;
        }
        if (features.isQuietEnvironmentMatch()) {
            score += QUIET_ENVIRONMENT_MATCH_WEIGHT;
        }
        return score;
    }

    private double roleScore(FriendRecommendationFeatures features) {
        String currentRole = normalize(features.getCurrentUserRole());
        String candidateRole = normalize(features.getCandidateRole());
        if (currentRole == null || candidateRole == null) {
            return 0.0;
        }
        if ("admin".equals(currentRole) || "admin".equals(candidateRole)) {
            return 0.0;
        }
        if ("parent".equals(currentRole) && "parent".equals(candidateRole)) {
            return PARENT_PARENT_ROLE_WEIGHT;
        }
        if (currentRole.equals(candidateRole)) {
            return SAME_ROLE_WEIGHT;
        }
        if (("parent".equals(currentRole) && "organizer".equals(candidateRole))
                || ("organizer".equals(currentRole) && "parent".equals(candidateRole))) {
            return PARENT_ORGANIZER_ROLE_WEIGHT;
        }
        return 0.0;
    }

    private double profileCompletenessScore(FriendRecommendationFeatures features) {
        double score = 0.0;
        if (features.isCandidateHasPhoto()) {
            score += HAS_PHOTO_WEIGHT;
        }
        if (features.isCandidateHasFullName()) {
            score += HAS_FULL_NAME_WEIGHT;
        }
        return score;
    }

    private double capped(int count, double weight, double maxScore) {
        return Math.min(Math.max(count, 0) * weight, maxScore);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}

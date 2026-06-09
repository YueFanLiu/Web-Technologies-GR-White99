package fr.isep.projectweb.model.algorithm.recommendation;

import fr.isep.projectweb.model.algorithm.recommendation.friend.FriendRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.friend.FriendRecommendationScorer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendRecommendationScorerTest {

    private final FriendRecommendationScorer scorer = new FriendRecommendationScorer();

    @Test
    void weightsMutualFriendsMoreThanSingleSharedActivity() {
        FriendRecommendationFeatures mutualFriendCandidate = baseFeatures();
        mutualFriendCandidate.setMutualFriendCount(1);

        FriendRecommendationFeatures activityOnlyCandidate = baseFeatures();
        activityOnlyCandidate.setSharedEventCount(1);

        assertTrue(scorer.score(mutualFriendCandidate) > scorer.score(activityOnlyCandidate));
    }

    @Test
    void addsAccessibilityMatchesToScore() {
        FriendRecommendationFeatures noAccessibilityMatch = baseFeatures();

        FriendRecommendationFeatures accessibilityMatch = baseFeatures();
        accessibilityMatch.setWheelchairAccessibleMatch(true);
        accessibilityMatch.setElevatorMatch(true);
        accessibilityMatch.setAccessibleRestroomMatch(true);
        accessibilityMatch.setQuietEnvironmentMatch(true);

        assertTrue(scorer.score(accessibilityMatch) > scorer.score(noAccessibilityMatch));
    }

    @Test
    void capsMutualFriendScore() {
        FriendRecommendationFeatures atCap = baseFeatures();
        atCap.setMutualFriendCount(5);

        FriendRecommendationFeatures overCap = baseFeatures();
        overCap.setMutualFriendCount(100);

        assertEquals(scorer.score(atCap), scorer.score(overCap), 0.001);
    }

    private FriendRecommendationFeatures baseFeatures() {
        FriendRecommendationFeatures features = new FriendRecommendationFeatures();
        features.setCurrentUserRole("PARENT");
        features.setCandidateRole("PARENT");
        features.setCandidateHasFullName(true);
        features.setCandidateHasPhoto(true);
        return features;
    }
}

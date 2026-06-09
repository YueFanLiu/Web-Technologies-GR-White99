package fr.isep.projectweb.model.algorithm.recommendation;

import fr.isep.projectweb.model.algorithm.recommendation.event.EventRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.event.EventRecommendationScorer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventRecommendationScorerTest {

    private final EventRecommendationScorer scorer = new EventRecommendationScorer();

    @Test
    void scoresEventHappeningNowHigherThanDistantFutureEvent() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 9, 10, 0);

        EventRecommendationFeatures happeningNow = baseFeatures(now);
        happeningNow.setStartTime(now.minusHours(1));
        happeningNow.setEndTime(now.plusHours(1));

        EventRecommendationFeatures future = baseFeatures(now);
        future.setStartTime(now.plusDays(60));
        future.setEndTime(now.plusDays(60).plusHours(2));

        assertTrue(scorer.score(happeningNow) > scorer.score(future));
    }

    @Test
    void capsRegistrationAndFavoritePopularityScores() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 9, 10, 0);
        EventRecommendationFeatures capped = baseFeatures(now);
        capped.setActiveRegistrationCount(1_000);
        capped.setFavoriteCount(1_000);

        EventRecommendationFeatures atCap = baseFeatures(now);
        atCap.setActiveRegistrationCount(50);
        atCap.setFavoriteCount(50);

        assertEquals(scorer.score(atCap), scorer.score(capped), 0.001);
    }

    @Test
    void penalizesCancelledAndDraftEvents() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 9, 10, 0);

        EventRecommendationFeatures published = baseFeatures(now);
        published.setStatus("PUBLISHED");

        EventRecommendationFeatures draft = baseFeatures(now);
        draft.setStatus("DRAFT");

        EventRecommendationFeatures cancelled = baseFeatures(now);
        cancelled.setStatus("CANCELLED");

        assertTrue(scorer.score(published) > scorer.score(draft));
        assertTrue(scorer.score(draft) > scorer.score(cancelled));
    }

    private EventRecommendationFeatures baseFeatures(LocalDateTime now) {
        EventRecommendationFeatures features = new EventRecommendationFeatures();
        features.setNow(now);
        features.setStartTime(now.plusDays(3));
        features.setEndTime(now.plusDays(3).plusHours(2));
        features.setTitle("Accessible Robotics Workshop");
        features.setDescription("Inclusive STEM activity");
        features.setCategory("Education");
        features.setStatus("PUBLISHED");
        features.setCapacity(20);
        features.setPrice(BigDecimal.ZERO);
        features.setHasLocation(true);
        features.setImageCount(1);
        features.setAverageRating(4.5);
        features.setReviewCount(3);
        return features;
    }
}

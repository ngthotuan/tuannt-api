package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ReviewGrade;
import com.tuannt.api.services.impl.ReviewScheduler.ReviewState;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSchedulerTest {
    private static final Instant NOW = Instant.parse("2026-10-27T08:00:00Z");
    private final ReviewScheduler scheduler = new ReviewScheduler(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void newCardPassesGraduateByGrade() {
        assertState(next(ReviewState.newCard(), ReviewGrade.HARD), 2.36, 1, 1, 0);
        assertState(next(ReviewState.newCard(), ReviewGrade.GOOD), 2.5, 1, 1, 0);
        assertState(next(ReviewState.newCard(), ReviewGrade.EASY), 2.6, 4, 1, 0);
        assertThat(next(ReviewState.newCard(), ReviewGrade.GOOD).dueAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
        assertThat(next(ReviewState.newCard(), ReviewGrade.EASY).dueAt()).isEqualTo(NOW.plus(Duration.ofDays(4)));
    }

    @Test
    void againOnANewCardIsALapseDueInTenMinutes() {
        ReviewState s = next(ReviewState.newCard(), ReviewGrade.AGAIN);
        assertState(s, 2.3, 0, 0, 1);
        assertThat(s.dueAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void secondStepIsSixDaysScaledByGrade() {
        ReviewState once = new ReviewState(2.5, 1, 1, 0, NOW);
        assertThat(next(once, ReviewGrade.HARD).intervalDays()).isEqualTo(5);
        assertThat(next(once, ReviewGrade.GOOD).intervalDays()).isEqualTo(6);
        assertThat(next(once, ReviewGrade.EASY).intervalDays()).isEqualTo(8);
        assertThat(next(once, ReviewGrade.GOOD).repetitions()).isEqualTo(2);
    }

    @Test
    void laterStepsMultiplyByThePreviousEase() {
        ReviewState twice = new ReviewState(2.5, 6, 2, 0, NOW);
        assertState(next(twice, ReviewGrade.HARD), 2.36, 12, 3, 0);
        assertState(next(twice, ReviewGrade.GOOD), 2.5, 15, 3, 0);
        assertState(next(twice, ReviewGrade.EASY), 2.6, 20, 3, 0);
        assertThat(next(twice, ReviewGrade.GOOD).dueAt()).isEqualTo(NOW.plus(Duration.ofDays(15)));
    }

    @Test
    void goodAnswersKeepGrowingTheInterval() {
        ReviewState s = ReviewState.newCard();
        int[] expected = {1, 6, 15, 38, 95};
        for (int interval : expected) {
            s = next(s, ReviewGrade.GOOD);
            assertThat(s.intervalDays()).isEqualTo(interval);
        }
        assertThat(s.ease()).isEqualTo(2.5);
        assertThat(s.repetitions()).isEqualTo(5);
    }

    @Test
    void easeNeverDropsBelowTheFloor() {
        assertThat(next(new ReviewState(1.35, 10, 3, 0, NOW), ReviewGrade.AGAIN).ease()).isEqualTo(1.3);
        assertThat(next(new ReviewState(1.3, 10, 3, 0, NOW), ReviewGrade.HARD).ease()).isEqualTo(1.3);
        ReviewState s = ReviewState.newCard();
        for (int i = 0; i < 20; i++) {
            s = next(s, ReviewGrade.AGAIN);
        }
        assertState(s, ReviewScheduler.MIN_EASE, 0, 0, 20);
    }

    @Test
    void aPassAlwaysPushesTheCardFurtherOut() {
        // 2 x 1.3 x 0.8 rounds back to 2, so the +1 day minimum applies.
        assertThat(next(new ReviewState(1.3, 2, 3, 0, NOW), ReviewGrade.HARD).intervalDays()).isEqualTo(3);
    }

    @Test
    void intervalIsCapped() {
        assertThat(next(new ReviewState(2.5, 3000, 9, 0, NOW), ReviewGrade.EASY).intervalDays())
                .isEqualTo(ReviewScheduler.MAX_INTERVAL_DAYS);
        assertThat(next(new ReviewState(2.5, Integer.MAX_VALUE, 9, 0, NOW), ReviewGrade.GOOD).intervalDays())
                .isEqualTo(ReviewScheduler.MAX_INTERVAL_DAYS);
    }

    @Test
    void lapseResetsProgressButKeepsCount() {
        ReviewState mature = new ReviewState(2.5, 30, 5, 1, NOW);
        ReviewState lapsed = next(mature, ReviewGrade.AGAIN);
        assertState(lapsed, 2.3, 0, 0, 2);
        // Relearning starts over at the first step, with the reduced ease.
        assertState(next(lapsed, ReviewGrade.GOOD), 2.3, 1, 1, 2);
    }

    @Test
    void streakCountsBackFromTodayOrYesterday() {
        assertThat(scheduler.streakDays(days("2026-10-27", "2026-10-26", "2026-10-25", "2026-10-23"))).isEqualTo(3);
        assertThat(scheduler.streakDays(days("2026-10-26", "2026-10-25"))).isEqualTo(2);
        assertThat(scheduler.streakDays(days("2026-10-25", "2026-10-24"))).isZero();
        assertThat(scheduler.streakDays(days("2026-10-27"))).isEqualTo(1);
        assertThat(scheduler.streakDays(days("2026-10-28", "2026-10-27", "2026-10-26"))).isEqualTo(2);
        assertThat(scheduler.streakDays(List.of())).isZero();
    }

    @Test
    void dayBoundariesAreUtc() {
        assertThat(scheduler.today()).isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(scheduler.endOfToday()).isEqualTo(Instant.parse("2026-10-28T00:00:00Z"));
    }

    private ReviewState next(ReviewState state, ReviewGrade grade) {
        return scheduler.next(state, grade);
    }

    private static List<LocalDate> days(String... isoDates) {
        return java.util.Arrays.stream(isoDates).map(LocalDate::parse).toList();
    }

    private static void assertState(ReviewState s, double ease, int interval, int repetitions, int lapses) {
        assertThat(s.ease()).isEqualTo(ease);
        assertThat(s.intervalDays()).isEqualTo(interval);
        assertThat(s.repetitions()).isEqualTo(repetitions);
        assertThat(s.lapses()).isEqualTo(lapses);
    }
}

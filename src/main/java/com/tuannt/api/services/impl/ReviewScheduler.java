package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ReviewGrade;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Simplified SM-2. Pure apart from the injected {@link Clock}, so it is unit-tested with a
 * fixed one. A new card starts at ease 2.5, interval 0, 0 repetitions, 0 lapses.
 * <p>
 * <b>AGAIN</b> (a lapse): repetitions = 0, lapses + 1, interval = 0, ease - 0.2, and the card is
 * due again {@link #RELEARN_DELAY} (10 minutes) later, so it comes back in the same session.
 * <p>
 * <b>HARD / GOOD / EASY</b> (a pass): repetitions + 1 and the card is due interval days later.
 * The interval depends on the repetitions <i>before</i> this answer and the ease <i>before</i>
 * this answer's adjustment:
 * <ul>
 *   <li>0 repetitions: HARD 1 day, GOOD 1 day, EASY 4 days;</li>
 *   <li>1 repetition: 6 days x grade factor, so HARD 5, GOOD 6, EASY 8;</li>
 *   <li>2 or more: previous interval x ease x grade factor, and at least previous + 1 so a pass
 *   always pushes the card further out.</li>
 * </ul>
 * Grade factors are HARD 0.8, GOOD 1.0, EASY 1.3; intervals round half up and are capped at
 * {@link #MAX_INTERVAL_DAYS}. Then the ease moves by the SM-2 formula
 * {@code 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)} with q = {@link ReviewGrade#getQuality()}:
 * HARD (q=3) -0.14, GOOD (q=4) 0, EASY (q=5) +0.1.
 * <p>
 * The ease is rounded to 2 decimals and never drops below {@link #MIN_EASE} (1.3).
 */
@Component
public class ReviewScheduler {
    public static final double INITIAL_EASE = 2.5;
    public static final double MIN_EASE = 1.3;
    public static final double AGAIN_EASE_PENALTY = 0.2;
    public static final Duration RELEARN_DELAY = Duration.ofMinutes(10);
    /** Keeps now + interval far from Instant/DATETIME limits after many EASY answers. */
    public static final int MAX_INTERVAL_DAYS = 3650;
    private static final int FIRST_INTERVAL_DAYS = 1;
    private static final int FIRST_EASY_INTERVAL_DAYS = 4;
    private static final int SECOND_INTERVAL_DAYS = 6;

    private final Clock clock;

    public ReviewScheduler(Clock clock) {
        this.clock = clock;
    }

    /** Scheduling state of one card; dueAt is null for a card never reviewed. */
    public record ReviewState(double ease, int intervalDays, int repetitions, int lapses, Instant dueAt) {
        public static ReviewState newCard() {
            return new ReviewState(INITIAL_EASE, 0, 0, 0, null);
        }
    }

    public Instant now() {
        return clock.instant();
    }

    /** Review days, streaks and "due today" all use UTC dates. */
    public LocalDate today() {
        return LocalDate.ofInstant(now(), ZoneOffset.UTC);
    }

    /** Start of tomorrow in UTC: anything due before it is due today. */
    public Instant endOfToday() {
        return today().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public ReviewState next(ReviewState current, ReviewGrade grade) {
        Instant now = now();
        if (grade == ReviewGrade.AGAIN) {
            return new ReviewState(clampEase(current.ease() - AGAIN_EASE_PENALTY), 0, 0,
                    current.lapses() + 1, now.plus(RELEARN_DELAY));
        }
        int interval = Math.min(passInterval(current, grade), MAX_INTERVAL_DAYS);
        int q = grade.getQuality();
        double ease = clampEase(current.ease() + 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        return new ReviewState(ease, interval, current.repetitions() + 1, current.lapses(),
                now.plus(Duration.ofDays(interval)));
    }

    private int passInterval(ReviewState current, ReviewGrade grade) {
        double factor = switch (grade) {
            case HARD -> 0.8;
            case EASY -> 1.3;
            default -> 1.0;
        };
        if (current.repetitions() == 0) {
            return grade == ReviewGrade.EASY ? FIRST_EASY_INTERVAL_DAYS : FIRST_INTERVAL_DAYS;
        }
        if (current.repetitions() == 1) {
            return (int) Math.round(SECOND_INTERVAL_DAYS * factor);
        }
        // Long arithmetic: a huge stored interval times ease must not overflow before the cap.
        long grown = Math.round(current.intervalDays() * current.ease() * factor);
        return (int) Math.min(Math.max(grown, current.intervalDays() + 1L), MAX_INTERVAL_DAYS);
    }

    private double clampEase(double ease) {
        return Math.max(MIN_EASE, Math.round(ease * 100) / 100.0);
    }

    /**
     * Consecutive UTC days with at least one review, counting back from today. A streak whose
     * last review was yesterday is still alive (today is not over yet); a gap of a whole day
     * ends it. reviewDaysDesc must be distinct dates, newest first.
     */
    public int streakDays(List<LocalDate> reviewDaysDesc) {
        LocalDate today = today();
        LocalDate expected = null;
        int streak = 0;
        for (LocalDate day : reviewDaysDesc) {
            if (day.isAfter(today)) {
                // Written by a clock ahead of ours; not part of the past streak.
                continue;
            }
            if (expected == null) {
                if (day.isBefore(today.minusDays(1))) {
                    return 0;
                }
                expected = day;
            }
            if (!day.equals(expected)) {
                break;
            }
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }
}

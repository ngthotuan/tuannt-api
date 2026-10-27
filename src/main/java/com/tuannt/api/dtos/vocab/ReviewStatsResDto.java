package com.tuannt.api.dtos.vocab;

import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Counts over every word the user has reviewed, regardless of saved state.
 */

@Getter
@Builder
public class ReviewStatsResDto extends BaseDto {
    private long dueNow;
    /** Due before the end of the current UTC day, including dueNow. */
    private long dueToday;
    /** Cards currently in a passing streak (repetitions >= 1). */
    private long learned;
    /** Cards with an interval of 21 days or more. */
    private long mature;
    /** Every answer ever given, AGAIN included. */
    private long totalReviews;
    /** Consecutive UTC days with a review, ending today or yesterday. */
    private int streakDays;
}

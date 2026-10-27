package com.tuannt.api.dtos.vocab;

import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * A word's review state right after an answer.
 */

@Getter
@Builder
public class ReviewStateDto extends BaseDto {
    private Long wordId;
    /** Epoch millis. */
    private Long dueAt;
    private int intervalDays;
    private int repetitions;
    private int lapses;
    private double ease;
}

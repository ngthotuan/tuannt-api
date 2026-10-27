package com.tuannt.api.dtos.vocab;

import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Getter
@Builder
public class ReviewQueueResDto extends BaseDto {
    /** Overdue cards first (oldest due first), then never-reviewed words. */
    private List<Item> items;
    /** All cards due now in this scope, which can exceed what fits in items. */
    private long dueCount;
    /** All never-reviewed words in this scope, not only the capped few in items. */
    private long newCount;

    @Getter
    @Builder
    public static class Item extends BaseDto {
        private VocabWordDto word;
        private Review review;
    }

    @Getter
    @Builder
    public static class Review extends BaseDto {
        /** Epoch millis; null for a new word. */
        private Long dueAt;
        private int intervalDays;
        private int repetitions;
        private int lapses;
        // A Boolean, not a boolean: Lombok names the primitive's getter isNew(), which Jackson
        // would publish as "new".
        private Boolean isNew;
    }
}

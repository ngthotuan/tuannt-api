package com.tuannt.api.services;

import com.tuannt.api.dtos.vocab.ReviewActivityDto;
import com.tuannt.api.dtos.vocab.ReviewQueueResDto;
import com.tuannt.api.dtos.vocab.ReviewReqDto;
import com.tuannt.api.dtos.vocab.ReviewStateDto;
import com.tuannt.api.dtos.vocab.ReviewStatsResDto;
import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 */
public interface VocabReviewService {
    /** savedOnly limits the queue to the user's saved words; otherwise every word is eligible. */
    ReviewQueueResDto queue(Long userId, int size, boolean savedOnly);

    ReviewStateDto review(Long userId, ReviewReqDto req);

    ReviewStatsResDto stats(Long userId);

    /** Answers per UTC day over the last {@code days} days, today included; empty days omitted. */
    List<ReviewActivityDto> activity(Long userId, int days);
}

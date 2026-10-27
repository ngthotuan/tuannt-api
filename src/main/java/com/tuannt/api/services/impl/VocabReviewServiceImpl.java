package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.dtos.vocab.ReviewQueueResDto;
import com.tuannt.api.dtos.vocab.ReviewReqDto;
import com.tuannt.api.dtos.vocab.ReviewStateDto;
import com.tuannt.api.dtos.vocab.ReviewStatsResDto;
import com.tuannt.api.dtos.vocab.VocabWordDto;
import com.tuannt.api.entities.VocabReview;
import com.tuannt.api.entities.VocabReviewLog;
import com.tuannt.api.entities.VocabWord;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.repositories.VocabReviewLogRepository;
import com.tuannt.api.repositories.VocabReviewRepository;
import com.tuannt.api.repositories.VocabWordRepository;
import com.tuannt.api.services.VocabReviewService;
import com.tuannt.api.services.impl.ReviewScheduler.ReviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Spaced-repetition review on top of the shared word list. The scheduling rules live in
 * ReviewScheduler; this class only loads and stores state.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabReviewServiceImpl implements VocabReviewService {
    /** At most this many never-seen words per queue, so a session is not all new cards. */
    static final int MAX_NEW_PER_QUEUE = 10;
    static final int MATURE_INTERVAL_DAYS = 21;
    /** How far back the streak looks; longer streaks are reported as this many days. */
    static final int STREAK_LOOKBACK_DAYS = 730;

    private final VocabWordRepository wordRepository;
    private final VocabReviewRepository reviewRepository;
    private final VocabReviewLogRepository reviewLogRepository;
    private final ReviewScheduler scheduler;

    @Override
    @Transactional(readOnly = true)
    public ReviewQueueResDto queue(Long userId, int size, boolean savedOnly) {
        Instant now = scheduler.now();
        List<VocabReview> due = reviewRepository.findDue(userId, now, savedOnly, PageRequest.of(0, size));
        Map<Long, VocabWord> dueWords = wordRepository.findAllById(due.stream().map(VocabReview::getWordId).toList())
                .stream().collect(Collectors.toMap(VocabWord::getId, Function.identity()));

        List<ReviewQueueResDto.Item> items = new ArrayList<>();
        for (VocabReview review : due) {
            VocabWord word = dueWords.get(review.getWordId());
            // Word deletes remove their reviews in the same transaction; this only guards a race.
            if (word != null) {
                items.add(item(word, review));
            }
        }

        int newLimit = Math.min(size - items.size(), MAX_NEW_PER_QUEUE);
        if (newLimit > 0) {
            wordRepository.findUnreviewed(userId, savedOnly, PageRequest.of(0, newLimit))
                    .forEach(word -> items.add(item(word, null)));
        }

        return ReviewQueueResDto.builder()
                .items(items)
                .dueCount(reviewRepository.countDue(userId, now, savedOnly))
                .newCount(wordRepository.countUnreviewed(userId, savedOnly))
                .build();
    }

    @Override
    @Transactional
    public ReviewStateDto review(Long userId, ReviewReqDto req) {
        Long wordId = req.getWordId();
        if (!wordRepository.existsById(wordId)) {
            throw new NotFoundException(ApiStatus.WORD_NOT_FOUND);
        }
        Instant now = scheduler.now();
        // No row lock: two concurrent first answers both insert and the unique key turns the
        // loser into a 409 (GlobalExceptionHandler); a concurrent double answer on an existing
        // row computes the same next state from the same snapshot, so last write wins harmlessly.
        VocabReview review = reviewRepository.findByUserIdAndWordId(userId, wordId).orElseGet(() -> {
            VocabReview created = new VocabReview();
            created.setUserId(userId);
            created.setWordId(wordId);
            created.setCreatedAt(now);
            ReviewState initial = ReviewState.newCard();
            created.setEase(initial.ease());
            return created;
        });

        ReviewState next = scheduler.next(new ReviewState(review.getEase(), review.getIntervalDays(),
                review.getRepetitions(), review.getLapses(), review.getDueAt()), req.getGrade());
        review.setEase(next.ease());
        review.setIntervalDays(next.intervalDays());
        review.setRepetitions(next.repetitions());
        review.setLapses(next.lapses());
        review.setDueAt(next.dueAt());
        review.setLastReviewedAt(now);
        review.setUpdatedAt(now);
        reviewRepository.save(review);

        VocabReviewLog entry = new VocabReviewLog();
        entry.setUserId(userId);
        entry.setWordId(wordId);
        entry.setGrade(req.getGrade());
        entry.setReviewedAt(now);
        entry.setReviewDay(scheduler.today());
        reviewLogRepository.save(entry);

        return ReviewStateDto.builder()
                .wordId(wordId)
                .dueAt(review.getDueAt().toEpochMilli())
                .intervalDays(review.getIntervalDays())
                .repetitions(review.getRepetitions())
                .lapses(review.getLapses())
                .ease(review.getEase())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatsResDto stats(Long userId) {
        LocalDate today = scheduler.today();
        List<LocalDate> days = reviewLogRepository.findReviewDays(userId, today.minusDays(STREAK_LOOKBACK_DAYS));
        return ReviewStatsResDto.builder()
                .dueNow(reviewRepository.countDue(userId, scheduler.now(), false))
                .dueToday(reviewRepository.countByUserIdAndDueAtBefore(userId, scheduler.endOfToday()))
                .learned(reviewRepository.countByUserIdAndRepetitionsGreaterThanEqual(userId, 1))
                .mature(reviewRepository.countByUserIdAndIntervalDaysGreaterThanEqual(userId, MATURE_INTERVAL_DAYS))
                .totalReviews(reviewLogRepository.countByUserId(userId))
                .streakDays(scheduler.streakDays(days))
                .build();
    }

    private ReviewQueueResDto.Item item(VocabWord word, VocabReview review) {
        ReviewQueueResDto.Review state = review == null
                ? ReviewQueueResDto.Review.builder().isNew(true).build()
                : ReviewQueueResDto.Review.builder()
                .dueAt(review.getDueAt().toEpochMilli())
                .intervalDays(review.getIntervalDays())
                .repetitions(review.getRepetitions())
                .lapses(review.getLapses())
                .isNew(false)
                .build();
        return ReviewQueueResDto.Item.builder()
                .word(VocabWordDto.builder().id(word.getId()).en(word.getEn()).vi(word.getVi()).build())
                .review(state)
                .build();
    }
}

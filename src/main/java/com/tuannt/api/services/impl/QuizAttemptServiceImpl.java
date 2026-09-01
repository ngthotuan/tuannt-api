package com.tuannt.api.services.impl;

import com.tuannt.api.dtos.quiz.QuizAttemptReqDto;
import com.tuannt.api.dtos.quiz.QuizHistoryResDto;
import com.tuannt.api.entities.QuizAttempt;
import com.tuannt.api.entities.QuizAttemptAnswer;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.TooManyRequestsException;
import com.tuannt.api.repositories.QuizAttemptRepository;
import com.tuannt.api.services.QuizAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptServiceImpl implements QuizAttemptService {
    private static final int MAX_PER_WINDOW = 60;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int MAX_WEAK_QUESTIONS = 10;

    private final QuizAttemptRepository repository;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional
    public Long record(QuizAttemptReqDto req, String ip) {
        if (!rateLimiter.tryAcquire(ip, MAX_PER_WINDOW, WINDOW)) {
            log.warn("Quiz attempt rate limited for ip: {}", ip);
            throw new TooManyRequestsException();
        }
        if (req.getCorrectAnswers() > req.getTotalQuestions()) {
            throw new BadRequestException("correctAnswers cannot exceed totalQuestions");
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setDeviceId(req.getDeviceId());
        attempt.setSourceName(req.getSourceName());
        attempt.setMode(req.getMode());
        attempt.setTotalQuestions(req.getTotalQuestions());
        attempt.setCorrectAnswers(req.getCorrectAnswers());
        attempt.setDurationSeconds(req.getDurationSeconds());
        attempt.setCreatedAt(Instant.now());
        attempt.setWrongAnswers(req.getWrongAnswers().stream().map(w -> {
            QuizAttemptAnswer answer = new QuizAttemptAnswer();
            answer.setQuestionIndex(w.getQuestionIndex());
            answer.setQuestion(w.getQuestion());
            answer.setSelectedText(w.getSelectedText());
            answer.setCorrectText(w.getCorrectText());
            return answer;
        }).toList());

        QuizAttempt saved = repository.save(attempt);
        log.info("Quiz attempt saved id: {} device: {} score: {}/{}",
                saved.getId(), saved.getDeviceId(), saved.getCorrectAnswers(), saved.getTotalQuestions());
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizHistoryResDto history(String deviceId, int limit) {
        List<QuizAttempt> attempts =
                repository.findByDeviceIdOrderByCreatedAtDesc(deviceId, PageRequest.of(0, limit));

        long totalQuestions = attempts.stream().mapToLong(QuizAttempt::getTotalQuestions).sum();
        long totalCorrect = attempts.stream().mapToLong(QuizAttempt::getCorrectAnswers).sum();

        QuizHistoryResDto.Stats stats = QuizHistoryResDto.Stats.builder()
                .totalAttempts(attempts.size())
                .totalQuestions(totalQuestions)
                .totalCorrect(totalCorrect)
                .averageScore(totalQuestions == 0 ? 0 : round(totalCorrect * 100.0 / totalQuestions))
                .bestScore(attempts.stream().mapToDouble(this::scoreOf).max().orElse(0))
                .build();

        return QuizHistoryResDto.builder()
                .stats(stats)
                .attempts(attempts.stream().map(this::toAttemptDto).toList())
                .weakQuestions(weakQuestions(attempts))
                .build();
    }

    /** Questions missed most often across the returned attempts, so review can be targeted. */
    private List<QuizHistoryResDto.WeakQuestion> weakQuestions(List<QuizAttempt> attempts) {
        Map<String, List<QuizAttemptAnswer>> grouped = attempts.stream()
                .flatMap(a -> a.getWrongAnswers().stream())
                .collect(Collectors.groupingBy(QuizAttemptAnswer::getQuestion));

        return grouped.entrySet().stream()
                .map(e -> QuizHistoryResDto.WeakQuestion.builder()
                        .question(e.getKey())
                        .correctText(e.getValue().get(0).getCorrectText())
                        .timesWrong(e.getValue().size())
                        .build())
                .sorted(Comparator.comparingLong(QuizHistoryResDto.WeakQuestion::getTimesWrong).reversed())
                .limit(MAX_WEAK_QUESTIONS)
                .toList();
    }

    private QuizHistoryResDto.Attempt toAttemptDto(QuizAttempt attempt) {
        return QuizHistoryResDto.Attempt.builder()
                .id(attempt.getId())
                .sourceName(attempt.getSourceName())
                .mode(attempt.getMode())
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .score(scoreOf(attempt))
                .durationSeconds(attempt.getDurationSeconds())
                .createdAt(attempt.getCreatedAt().toEpochMilli())
                .wrongAnswers(attempt.getWrongAnswers().stream()
                        .map(w -> QuizHistoryResDto.WrongAnswer.builder()
                                .questionIndex(w.getQuestionIndex())
                                .question(w.getQuestion())
                                .selectedText(w.getSelectedText())
                                .correctText(w.getCorrectText())
                                .build())
                        .toList())
                .build();
    }

    private double scoreOf(QuizAttempt attempt) {
        if (attempt.getTotalQuestions() == 0) return 0;
        return round(attempt.getCorrectAnswers() * 100.0 / attempt.getTotalQuestions());
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}

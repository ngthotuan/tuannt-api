package com.tuannt.api.dtos.quiz;

import com.tuannt.api.dtos.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizHistoryResDto extends BaseDto {
    private Stats stats;
    private List<Attempt> attempts;
    private List<WeakQuestion> weakQuestions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats extends BaseDto {
        private long totalAttempts;
        private long totalQuestions;
        private long totalCorrect;
        private double averageScore;
        private double bestScore;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attempt extends BaseDto {
        private Long id;
        private String sourceName;
        private String mode;
        private int totalQuestions;
        private int correctAnswers;
        private double score;
        private Integer durationSeconds;
        /** Epoch millis, same convention as the article feed timestamps. */
        private Long createdAt;
        private List<WrongAnswer> wrongAnswers;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WrongAnswer extends BaseDto {
        private int questionIndex;
        private String question;
        private String selectedText;
        private String correctText;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakQuestion extends BaseDto {
        private String question;
        private String correctText;
        private long timesWrong;
    }
}

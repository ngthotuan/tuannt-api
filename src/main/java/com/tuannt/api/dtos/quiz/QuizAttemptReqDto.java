package com.tuannt.api.dtos.quiz;

import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Getter
@Setter
public class QuizAttemptReqDto extends BaseDto {
    @NotBlank(message = "deviceId is required")
    @Size(max = 64, message = "deviceId must be at most 64 characters")
    private String deviceId;

    @NotBlank(message = "sourceName is required")
    @Size(max = 200, message = "sourceName must be at most 200 characters")
    private String sourceName;

    @NotBlank(message = "mode is required")
    @Pattern(regexp = "study|exam", message = "mode must be study or exam")
    private String mode;

    @NotNull(message = "totalQuestions is required")
    @Min(value = 1, message = "totalQuestions must be at least 1")
    private Integer totalQuestions;

    @NotNull(message = "correctAnswers is required")
    @Min(value = 0, message = "correctAnswers must be at least 0")
    private Integer correctAnswers;

    @Min(value = 0, message = "durationSeconds must be at least 0")
    private Integer durationSeconds;

    @Valid
    @Size(max = 500, message = "too many wrong answers")
    private List<WrongAnswer> wrongAnswers = new ArrayList<>();

    @Getter
    @Setter
    public static class WrongAnswer extends BaseDto {
        @NotNull(message = "questionIndex is required")
        @Min(value = 0, message = "questionIndex must be at least 0")
        private Integer questionIndex;

        @NotBlank(message = "question is required")
        @Size(max = 1000, message = "question must be at most 1000 characters")
        private String question;

        @Size(max = 500, message = "selectedText must be at most 500 characters")
        private String selectedText;

        @Size(max = 500, message = "correctText must be at most 500 characters")
        private String correctText;
    }
}

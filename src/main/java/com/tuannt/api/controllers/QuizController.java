package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.quiz.QuizAttemptReqDto;
import com.tuannt.api.dtos.quiz.QuizHistoryResDto;
import com.tuannt.api.services.QuizAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_QUIZ_PATH)
@RequiredArgsConstructor
public class QuizController {
    private final QuizAttemptService quizAttemptService;
    private final HttpServletRequest request;

    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> record(@Valid @RequestBody QuizAttemptReqDto req) {
        return Map.of("id", quizAttemptService.record(req, clientIp()));
    }

    @GetMapping("/attempts")
    public QuizHistoryResDto history(
            @RequestParam @NotBlank(message = "deviceId is required")
            @Size(max = 64, message = "deviceId must be at most 64 characters") String deviceId,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 200, message = "limit must be at most 200") int limit) {
        return quizAttemptService.history(deviceId, limit);
    }

    /** Heroku terminates TLS at its router, so the caller only survives in X-Forwarded-For. */
    private String clientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

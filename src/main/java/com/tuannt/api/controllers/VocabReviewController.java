package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.vocab.ReviewQueueResDto;
import com.tuannt.api.dtos.vocab.ReviewReqDto;
import com.tuannt.api.dtos.vocab.ReviewStateDto;
import com.tuannt.api.dtos.vocab.ReviewStatsResDto;
import com.tuannt.api.services.VocabReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Spaced-repetition review. Every path here needs a logged-in user (SecurityConfig), so the
 * Jwt is never null.
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_VOCAB_REVIEW_PATH)
@RequiredArgsConstructor
public class VocabReviewController {
    private static final String SCOPE_SAVED = "saved";

    private final VocabReviewService reviewService;

    @GetMapping("/queue")
    public ReviewQueueResDto queue(
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(defaultValue = SCOPE_SAVED)
            @Pattern(regexp = "saved|all", message = "scope must be saved or all") String scope,
            @AuthenticationPrincipal Jwt jwt) {
        return reviewService.queue(userId(jwt), size, SCOPE_SAVED.equals(scope));
    }

    @PostMapping
    public ReviewStateDto review(@Valid @RequestBody ReviewReqDto req, @AuthenticationPrincipal Jwt jwt) {
        return reviewService.review(userId(jwt), req);
    }

    @GetMapping("/stats")
    public ReviewStatsResDto stats(@AuthenticationPrincipal Jwt jwt) {
        return reviewService.stats(userId(jwt));
    }

    /** The token subject is the app_user id, set at login. */
    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

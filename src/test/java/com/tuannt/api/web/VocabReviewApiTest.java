package com.tuannt.api.web;

import com.tuannt.api.configs.AuthConfig;
import com.tuannt.api.configs.CorsConfig;
import com.tuannt.api.configs.GlobalConfig;
import com.tuannt.api.configs.SecurityConfig;
import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.constants.ReviewGrade;
import com.tuannt.api.controllers.VocabReviewController;
import com.tuannt.api.dtos.vocab.ReviewQueueResDto;
import com.tuannt.api.dtos.vocab.ReviewReqDto;
import com.tuannt.api.dtos.vocab.ReviewStateDto;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.services.VocabReviewService;
import com.tuannt.api.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access rules and validation for the review endpoints. Services are mocked, so this runs
 * without a database.
 */
@WebMvcTest(controllers = VocabReviewController.class)
@Import({SecurityConfig.class, GlobalConfig.class, CommonUtil.class, VocabReviewApiTest.Properties.class})
@TestPropertySource(properties = {
        "auth.jwtSecret=test-secret-that-is-at-least-32-bytes-long",
        "cors.allowedOriginPatterns=https://nttuan.dev",
})
class VocabReviewApiTest {
    private static final RequestPostProcessor USER =
            jwt().jwt(j -> j.subject("7")).authorities(new SimpleGrantedAuthority("ROLE_USER"));

    @TestConfiguration
    @EnableConfigurationProperties({AuthConfig.class, CorsConfig.class})
    static class Properties {
    }

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private VocabReviewService reviewService;

    @Test
    void everyReviewEndpointNeedsAToken() throws Exception {
        mvc.perform(get("/v1/vocab/review/queue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        mvc.perform(get("/v1/vocab/review/stats")).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/vocab/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wordId\":1,\"grade\":\"GOOD\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(reviewService);
    }

    @Test
    void queueDefaultsToTwentySavedWordsOfTheTokenUser() throws Exception {
        when(reviewService.queue(any(), eq(20), eq(true)))
                .thenReturn(ReviewQueueResDto.builder().items(List.of(ReviewQueueResDto.Item.builder()
                        .review(ReviewQueueResDto.Review.builder().isNew(true).build()).build())).build());
        mvc.perform(get("/v1/vocab/review/queue").with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].review.isNew").value(true))
                .andExpect(jsonPath("$.items[0].review.new").doesNotExist());
        verify(reviewService).queue(7L, 20, true);

        mvc.perform(get("/v1/vocab/review/queue").param("scope", "all").param("size", "5").with(USER))
                .andExpect(status().isOk());
        verify(reviewService).queue(7L, 5, false);
    }

    @Test
    void queueSizeAndScopeAreBounded() throws Exception {
        mvc.perform(get("/v1/vocab/review/queue").param("size", "0").with(USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("size: size must be at least 1"));
        mvc.perform(get("/v1/vocab/review/queue").param("size", "101").with(USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("size: size must be at most 100"));
        mvc.perform(get("/v1/vocab/review/queue").param("scope", "everything").with(USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("scope: scope must be saved or all"));
        verifyNoInteractions(reviewService);
    }

    @Test
    void badOrMissingGradeIsABadRequest() throws Exception {
        mvc.perform(post("/v1/vocab/review").with(USER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wordId\":1,\"grade\":\"PERFECT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
        mvc.perform(post("/v1/vocab/review").with(USER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wordId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("grade must be AGAIN, HARD, GOOD or EASY"));
        mvc.perform(post("/v1/vocab/review").with(USER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"GOOD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("wordId is required"));
        verifyNoInteractions(reviewService);
    }

    @Test
    void reviewReturnsTheNewStateAsEpochMillis() throws Exception {
        when(reviewService.review(eq(7L), argThat(r -> r.getWordId() == 3L && r.getGrade() == ReviewGrade.GOOD)))
                .thenReturn(ReviewStateDto.builder().wordId(3L).dueAt(1_800_000_000_000L)
                        .intervalDays(1).repetitions(1).ease(2.5).build());
        mvc.perform(post("/v1/vocab/review").with(USER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wordId\":3,\"grade\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueAt").value(1_800_000_000_000L))
                .andExpect(jsonPath("$.ease").value(2.5));
    }

    @Test
    void unknownWordIsNotFound() throws Exception {
        when(reviewService.review(eq(7L), any(ReviewReqDto.class))).thenThrow(new NotFoundException(ApiStatus.WORD_NOT_FOUND));
        mvc.perform(post("/v1/vocab/review").with(USER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wordId\":999,\"grade\":\"AGAIN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WORD_NOT_FOUND"));
    }

    @Test
    void concurrentFirstReviewBecomesConflict() throws Exception {
        when(reviewService.review(eq(7L), any(ReviewReqDto.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));
        mvc.perform(post("/v1/vocab/review").with(USER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wordId\":3,\"grade\":\"GOOD\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }
}

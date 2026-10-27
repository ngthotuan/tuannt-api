package com.tuannt.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuannt.api.repositories.VocabReviewLogRepository;
import com.tuannt.api.repositories.VocabReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spaced-repetition review against a real MySQL migrated by Flyway, with the application clock
 * replaced by one the test moves forward. Needs Docker.
 */
@SpringBootTest(properties = {
        "auth.jwtSecret=integration-secret-that-is-at-least-32-bytes",
        "auth.admin.username=it-admin",
        "auth.admin.password=it-admin-password",
        // Satisfy the ${...} placeholders in application.yaml; @ServiceConnection supplies the real ones.
        "MYSQL_HOST=unused", "MYSQL_PORT=3306", "MYSQL_DATABASE=unused",
        "MYSQL_USERNAME=unused", "MYSQL_PASSWORD=unused",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class VocabReviewFlowIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static final MutableClock CLOCK = new MutableClock();
    /** A distinct client IP per login keeps the per-IP login limit out of the way. */
    private static final AtomicInteger LOGIN_IP = new AtomicInteger(1);

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private VocabReviewRepository reviewRepository;
    @Autowired
    private VocabReviewLogRepository reviewLogRepository;

    @Test
    void queuePutsOverdueCardsBeforeNewWordsAndCapsTheNewOnes() throws Exception {
        Instant t0 = Instant.parse("2026-10-27T08:00:00Z");
        CLOCK.set(t0);
        String admin = login("it-admin", "it-admin-password");
        String user = createUserAndLogin(admin, "rv-queue");
        List<Long> ids = addWords(admin, "rvq", 14);
        for (Long id : ids) {
            mvc.perform(put("/v1/vocab/saved/" + id).header("Authorization", user)).andExpect(status().isNoContent());
        }

        // Nothing reviewed yet: only new words, capped at 10, oldest first.
        mvc.perform(get("/v1/vocab/review/queue").header("Authorization", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.items[0].word.id").value(ids.get(0)))
                .andExpect(jsonPath("$.items[0].review.isNew").value(true))
                .andExpect(jsonPath("$.dueCount").value(0))
                .andExpect(jsonPath("$.newCount").value(14));

        review(user, ids.get(5), "AGAIN").andExpect(status().isOk());
        CLOCK.set(t0.plus(Duration.ofMinutes(1)));
        review(user, ids.get(2), "AGAIN").andExpect(status().isOk());
        review(user, ids.get(0), "GOOD").andExpect(status().isOk());

        CLOCK.set(t0.plus(Duration.ofMinutes(15)));
        mvc.perform(get("/v1/vocab/review/queue").param("size", "20").header("Authorization", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(12))
                .andExpect(jsonPath("$.items[0].word.id").value(ids.get(5)))
                .andExpect(jsonPath("$.items[0].review.isNew").value(false))
                .andExpect(jsonPath("$.items[0].review.lapses").value(1))
                .andExpect(jsonPath("$.items[0].review.dueAt").value(t0.plus(Duration.ofMinutes(10)).toEpochMilli()))
                .andExpect(jsonPath("$.items[1].word.id").value(ids.get(2)))
                // ids[0] is not due for a day, so the first new word is ids[1].
                .andExpect(jsonPath("$.items[2].word.id").value(ids.get(1)))
                .andExpect(jsonPath("$.items[2].review.isNew").value(true))
                .andExpect(jsonPath("$.dueCount").value(2))
                .andExpect(jsonPath("$.newCount").value(11));

        mvc.perform(get("/v1/vocab/review/queue").param("size", "1").header("Authorization", user))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].word.id").value(ids.get(5)))
                .andExpect(jsonPath("$.dueCount").value(2));

        // Unsaving a word takes it out of the saved scope but not out of "all".
        mvc.perform(delete("/v1/vocab/saved/" + ids.get(5)).header("Authorization", user)).andExpect(status().isNoContent());
        mvc.perform(get("/v1/vocab/review/queue").header("Authorization", user))
                .andExpect(jsonPath("$.items[0].word.id").value(ids.get(2)))
                .andExpect(jsonPath("$.dueCount").value(1));
        JsonNode all = body(mvc.perform(get("/v1/vocab/review/queue").param("scope", "all").header("Authorization", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].word.id").value(ids.get(5)))
                .andExpect(jsonPath("$.dueCount").value(2)));
        // The 68 seeded words are never reviewed by this user, so "all" has far more new words.
        assertThat(all.path("newCount").asLong()).isGreaterThanOrEqualTo(68 + 11);
        assertThat(all.path("items").size()).isEqualTo(12);
    }

    @Test
    void gradingRoundTripUpdatesStateAndStats() throws Exception {
        Instant t0 = Instant.parse("2026-11-02T09:00:00Z");
        CLOCK.set(t0);
        String admin = login("it-admin", "it-admin-password");
        String user = createUserAndLogin(admin, "rv-grade");
        long wordId = addWords(admin, "rvg", 1).get(0);

        review(user, wordId, "GOOD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordId").value(wordId))
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.repetitions").value(1))
                .andExpect(jsonPath("$.lapses").value(0))
                .andExpect(jsonPath("$.ease").value(2.5))
                .andExpect(jsonPath("$.dueAt").value(t0.plus(Duration.ofDays(1)).toEpochMilli()));

        CLOCK.set(t0.plus(Duration.ofDays(1)));
        review(user, wordId, "GOOD").andExpect(jsonPath("$.intervalDays").value(6));
        review(user, wordId, "EASY")
                .andExpect(jsonPath("$.intervalDays").value(20))
                .andExpect(jsonPath("$.repetitions").value(3))
                .andExpect(jsonPath("$.ease").value(2.6));

        stats(user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueNow").value(0))
                .andExpect(jsonPath("$.dueToday").value(0))
                .andExpect(jsonPath("$.learned").value(1))
                .andExpect(jsonPath("$.mature").value(0))
                .andExpect(jsonPath("$.totalReviews").value(3))
                .andExpect(jsonPath("$.streakDays").value(2));

        review(user, wordId, "AGAIN")
                .andExpect(jsonPath("$.intervalDays").value(0))
                .andExpect(jsonPath("$.repetitions").value(0))
                .andExpect(jsonPath("$.lapses").value(1))
                .andExpect(jsonPath("$.ease").value(2.4));
        stats(user)
                .andExpect(jsonPath("$.dueNow").value(0))
                .andExpect(jsonPath("$.dueToday").value(1))
                .andExpect(jsonPath("$.learned").value(0))
                .andExpect(jsonPath("$.totalReviews").value(4));

        // A whole day without reviews breaks the streak; the lapsed card is overdue by then.
        CLOCK.set(t0.plus(Duration.ofDays(3)));
        stats(user)
                .andExpect(jsonPath("$.dueNow").value(1))
                .andExpect(jsonPath("$.streakDays").value(0));

        review(user, 999_999L, "GOOD")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("WORD_NOT_FOUND"));
    }

    @Test
    void deletingAWordOrAUserRemovesTheirReviewRows() throws Exception {
        CLOCK.set(Instant.parse("2026-11-10T12:00:00Z"));
        String admin = login("it-admin", "it-admin-password");
        JsonNode created = body(mvc.perform(post("/v1/admin/users").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rv-clean\",\"password\":\"rv-clean-password\",\"role\":\"USER\"}"))
                .andExpect(status().isCreated()));
        long userId = created.path("id").asLong();
        String user = login("rv-clean", "rv-clean-password");
        List<Long> ids = addWords(admin, "rvc", 2);
        review(user, ids.get(0), "GOOD").andExpect(status().isOk());
        review(user, ids.get(1), "HARD").andExpect(status().isOk());
        review(admin, ids.get(0), "EASY").andExpect(status().isOk());

        mvc.perform(delete("/v1/vocab/words/" + ids.get(0)).header("Authorization", admin)).andExpect(status().isNoContent());
        assertThat(reviewRepository.findAll()).noneMatch(r -> r.getWordId().equals(ids.get(0)));
        assertThat(reviewLogRepository.findAll()).noneMatch(l -> l.getWordId().equals(ids.get(0)));
        stats(user).andExpect(jsonPath("$.totalReviews").value(1)).andExpect(jsonPath("$.learned").value(1));

        mvc.perform(delete("/v1/admin/users/" + userId).header("Authorization", admin)).andExpect(status().isNoContent());
        assertThat(reviewRepository.findAll()).noneMatch(r -> r.getUserId().equals(userId));
        assertThat(reviewLogRepository.findAll()).noneMatch(l -> l.getUserId().equals(userId));
    }

    private ResultActions review(String bearer, long wordId, String grade) throws Exception {
        return mvc.perform(post("/v1/vocab/review").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"wordId\":" + wordId + ",\"grade\":\"" + grade + "\"}"));
    }

    private ResultActions stats(String bearer) throws Exception {
        return mvc.perform(get("/v1/vocab/review/stats").header("Authorization", bearer));
    }

    private List<Long> addWords(String admin, String prefix, int count) throws Exception {
        StringBuilder words = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            words.append(i > 1 ? "," : "").append(String.format("{\"en\":\"%s-%02d\",\"vi\":\"nghia %d\"}", prefix, i, i));
        }
        JsonNode created = body(mvc.perform(post("/v1/vocab/words").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"words\":[" + words + "]}"))
                .andExpect(status().isCreated())).path("created");
        List<Long> ids = new ArrayList<>();
        created.forEach(w -> ids.add(w.path("id").asLong()));
        assertThat(ids).hasSize(count).isSorted();
        return ids;
    }

    private String createUserAndLogin(String admin, String username) throws Exception {
        mvc.perform(post("/v1/admin/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "-password\",\"role\":\"USER\"}"))
                .andExpect(status().isCreated());
        return login(username, username + "-password");
    }

    private String login(String username, String password) throws Exception {
        JsonNode body = body(mvc.perform(post("/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100." + LOGIN_IP.getAndIncrement())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()));
        return "Bearer " + body.path("token").asText();
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** A clock the test moves by hand; review scheduling reads "now" only through it. */
    static final class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-10-27T00:00:00Z");

        void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

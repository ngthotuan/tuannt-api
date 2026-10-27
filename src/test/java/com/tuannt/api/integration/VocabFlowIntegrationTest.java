package com.tuannt.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole stack against a real MySQL: Flyway from an empty schema, the bootstrap admin, a
 * real login and the vocab flows. Needs Docker, so it runs in CI and is skipped on machines
 * without it.
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
class VocabFlowIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void migrationsSeedTheSheetWords() throws Exception {
        mvc.perform(get("/v1/vocab/words").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(68))
                .andExpect(jsonPath("$.items.length()").value(5));
        mvc.perform(get("/v1/vocab/words/sequence").param("size", "2"))
                .andExpect(jsonPath("$.items[0].en").value("University education"));
        mvc.perform(get("/v1/vocab/words/random").param("size", "10"))
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.total").value(68));
    }

    @Test
    void adminCanAddSaveAndDeleteAWord() throws Exception {
        String bearer = "Bearer " + login();

        String added = mvc.perform(post("/v1/vocab/words").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"words\":[{\"en\":\"it-word\",\"vi\":\"tu kiem thu\"},{\"en\":\"IT-WORD\",\"vi\":\"dup\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created.length()").value(1))
                .andExpect(jsonPath("$.skipped[0]").value("IT-WORD"))
                .andReturn().getResponse().getContentAsString();
        long wordId = objectMapper.readTree(added).path("created").get(0).path("id").asLong();

        mvc.perform(put("/v1/vocab/saved/" + wordId).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/v1/vocab/words").param("saved", "true").header("Authorization", bearer))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(wordId));

        mvc.perform(delete("/v1/vocab/words/" + wordId).header("Authorization", bearer)).andExpect(status().isNoContent());
        // Deleting a word must also drop it from every saved list.
        mvc.perform(get("/v1/vocab/saved").header("Authorization", bearer)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it-admin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void healthAndApiDocsAreServed() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        String docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode paths = objectMapper.readTree(docs).path("paths");
        assertThat(paths.has("/v1/vocab/words")).isTrue();
        assertThat(paths.has("/v1/adyen/createOrder")).isFalse();
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it-admin\",\"password\":\"it-admin-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("token").asText();
    }
}

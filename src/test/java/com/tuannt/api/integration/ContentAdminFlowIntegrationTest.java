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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Blog, portfolio, inbox and account management against a real MySQL migrated from empty by
 * Flyway. Needs Docker.
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
class ContentAdminFlowIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void draftIsHiddenUntilPublished() throws Exception {
        String admin = bearer("it-admin", "it-admin-password");
        JsonNode created = body(mvc.perform(json(post("/v1/admin/posts"), admin, """
                        {"title":"Học Spring Boot ở Đà Nẵng","excerpt":"Ghi chú","content":"# Xin chào\\n\\nNội dung.",
                         "tags":["Spring Boot","Java"],"status":"DRAFT"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("hoc-spring-boot-o-da-nang"))
                .andExpect(jsonPath("$.tags[0]").value("spring-boot"))
                .andExpect(jsonPath("$.publishedAt").doesNotExist()));
        long id = created.path("id").asLong();

        mvc.perform(get("/v1/posts/hoc-spring-boot-o-da-nang"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
        mvc.perform(get("/v1/admin/posts").param("status", "DRAFT").header("Authorization", admin))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].content").doesNotExist());

        // Same derived slug again is a clear 400, not a 500 from the unique key.
        mvc.perform(json(post("/v1/admin/posts"), admin, "{\"title\":\"Học Spring Boot ở Đà Nẵng\",\"content\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SLUG_TAKEN"));

        mvc.perform(json(put("/v1/admin/posts/" + id), admin, """
                        {"title":"Học Spring Boot ở Đà Nẵng","content":"# Xin chào","tags":["java","spring-boot"],"status":"PUBLISHED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("hoc-spring-boot-o-da-nang"))
                .andExpect(jsonPath("$.publishedAt").isNumber());

        mvc.perform(get("/v1/posts/hoc-spring-boot-o-da-nang"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("# Xin chào"));
        mvc.perform(get("/v1/posts").param("tag", "Spring Boot"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("hoc-spring-boot-o-da-nang"))
                .andExpect(jsonPath("$.items[0].content").doesNotExist());
        mvc.perform(get("/v1/posts").param("tag", "spring")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/v1/posts/tags"))
                .andExpect(jsonPath("$[?(@.tag == 'java')].count").value(1));

        mvc.perform(delete("/v1/admin/posts/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(get("/v1/posts/hoc-spring-boot-o-da-nang")).andExpect(status().isNotFound());
    }

    @Test
    void hiddenProjectsAreAdminOnly() throws Exception {
        String admin = bearer("it-admin", "it-admin-password");
        mvc.perform(json(post("/v1/admin/projects"), admin, """
                        {"name":"Ứng dụng Quiz","summary":"Quiz app","tech":["React","TypeScript","react"],
                         "repoUrl":"https://github.com/ngthotuan/tuannt-fe","featured":true,"sortOrder":2}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("ung-dung-quiz"))
                .andExpect(jsonPath("$.tech.length()").value(2));
        String hidden = body(mvc.perform(json(post("/v1/admin/projects"), admin,
                        "{\"name\":\"Secret\",\"summary\":\"wip\",\"visible\":false}"))
                .andExpect(status().isCreated())).path("id").asText();

        mvc.perform(get("/v1/projects"))
                .andExpect(jsonPath("$[?(@.slug == 'ung-dung-quiz')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'secret')]").doesNotExist());
        mvc.perform(get("/v1/admin/projects").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.slug == 'secret')]").exists());
        mvc.perform(delete("/v1/admin/projects/" + hidden).header("Authorization", admin)).andExpect(status().isNoContent());
    }

    @Test
    void inboxCanMarkReadAndDelete() throws Exception {
        String admin = bearer("it-admin", "it-admin-password");
        mvc.perform(post("/v1/contact").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Visitor","email":"visitor@example.com","subject":"Hello","message":"Hi there"}"""))
                .andExpect(status().isCreated());

        JsonNode unread = body(mvc.perform(get("/v1/admin/contact-messages").param("unread", "true")
                .header("Authorization", admin)).andExpect(status().isOk()));
        long id = unread.path("items").get(0).path("id").asLong();
        long unreadBefore = unread.path("total").asLong();

        mvc.perform(put("/v1/admin/contact-messages/" + id + "/read").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isNumber());
        mvc.perform(get("/v1/admin/contact-messages").param("unread", "true").header("Authorization", admin))
                .andExpect(jsonPath("$.total").value(unreadBefore - 1));
        mvc.perform(delete("/v1/admin/contact-messages/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(put("/v1/admin/contact-messages/" + id + "/read").header("Authorization", admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MESSAGE_NOT_FOUND"));
    }

    @Test
    void passwordChangeAndAdminGuards() throws Exception {
        String admin = bearer("it-admin", "it-admin-password");
        long itAdminId = body(mvc.perform(get("/v1/auth/me").header("Authorization", admin))).path("id").asLong();

        long userId = createUser(admin, "it-user", "user-password-1", "USER");
        String user = bearer("it-user", "user-password-1");
        mvc.perform(json(put("/v1/auth/password"), user, "{\"currentPassword\":\"wrong-password\",\"newPassword\":\"user-password-2\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        mvc.perform(json(put("/v1/auth/password"), user, "{\"currentPassword\":\"user-password-1\",\"newPassword\":\"user-password-2\"}"))
                .andExpect(status().isNoContent());
        bearer("it-user", "user-password-2");

        mvc.perform(json(put("/v1/admin/users/" + itAdminId + "/role"), admin, "{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_CHANGE_SELF"));

        // A second admin is demoted; its old token still carries the ADMIN claim, and using it to
        // remove the one real admin left must be refused.
        long secondId = createUser(admin, "it-admin-2", "admin-password-2", "ADMIN");
        String second = bearer("it-admin-2", "admin-password-2");
        mvc.perform(json(put("/v1/admin/users/" + secondId + "/role"), admin, "{\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(delete("/v1/admin/users/" + itAdminId).header("Authorization", second))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("LAST_ADMIN"));

        mvc.perform(get("/v1/admin/users").header("Authorization", admin))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").isNumber());
        mvc.perform(delete("/v1/admin/users/" + userId).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(delete("/v1/admin/users/" + secondId).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(get("/v1/auth/me").header("Authorization", user)).andExpect(status().isUnauthorized());
    }

    private long createUser(String admin, String username, String password, String role) throws Exception {
        return body(mvc.perform(json(post("/v1/admin/users"), admin,
                        "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    private String bearer(String username, String password) throws Exception {
        JsonNode login = body(mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()));
        return "Bearer " + login.path("token").asText();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String bearer, String body) {
        return builder.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}

package com.tuannt.api.web;

import com.tuannt.api.configs.AuthConfig;
import com.tuannt.api.configs.CorsConfig;
import com.tuannt.api.configs.GlobalConfig;
import com.tuannt.api.configs.SecurityConfig;
import com.tuannt.api.controllers.AdminController;
import com.tuannt.api.controllers.AuthController;
import com.tuannt.api.controllers.VocabController;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.services.AuthService;
import com.tuannt.api.services.VocabService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks in the access rules from SecurityConfig and the 4xx-not-500 behaviour that a review
 * found missing. Services are mocked, so this runs without a database.
 */
@WebMvcTest(controllers = {VocabController.class, AuthController.class, AdminController.class})
@Import({SecurityConfig.class, GlobalConfig.class, CommonUtil.class, ApiSecurityAndValidationTest.Properties.class})
@TestPropertySource(properties = {
        "auth.jwtSecret=test-secret-that-is-at-least-32-bytes-long",
        "cors.allowedOriginPatterns=https://nttuan.dev",
        "cors.allowedMethods=GET,POST,PUT,DELETE,OPTIONS",
})
class ApiSecurityAndValidationTest {
    private static final RequestPostProcessor ADMIN =
            jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final RequestPostProcessor USER =
            jwt().jwt(j -> j.subject("7")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    private static final String ONE_WORD = "{\"words\":[{\"en\":\"apple\",\"vi\":\"qua tao\"}]}";

    /** The slice skips @ConfigurationPropertiesScan, so bind the two property classes it needs. */
    @TestConfiguration
    @EnableConfigurationProperties({AuthConfig.class, CorsConfig.class})
    static class Properties {
    }

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private VocabService vocabService;
    @MockitoBean
    private AuthService authService;

    @Test
    void wordListIsPublic() throws Exception {
        when(vocabService.listWords(anyInt(), anyInt(), any(), anyBoolean(), any()))
                .thenReturn(PageResDto.<com.tuannt.api.dtos.vocab.VocabWordDto>builder().items(List.of()).build());
        mvc.perform(get("/v1/vocab/words")).andExpect(status().isOk());
        verify(vocabService).listWords(0, 50, null, false, null);
    }

    @Test
    void savedFilterNeedsLogin() throws Exception {
        mvc.perform(get("/v1/vocab/words").param("saved", "true"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void savedFilterUsesTheTokenSubject() throws Exception {
        mvc.perform(get("/v1/vocab/words").param("saved", "true").with(USER)).andExpect(status().isOk());
        verify(vocabService).listWords(0, 50, null, true, 7L);
    }

    @Test
    void addingWordsNeedsAToken() throws Exception {
        mvc.perform(post("/v1/vocab/words").contentType(MediaType.APPLICATION_JSON).content(ONE_WORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        verifyNoInteractions(vocabService);
    }

    @Test
    void addingWordsIsAdminOnly() throws Exception {
        mvc.perform(post("/v1/vocab/words").with(USER).contentType(MediaType.APPLICATION_JSON).content(ONE_WORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mvc.perform(delete("/v1/vocab/words/5").with(USER)).andExpect(status().isForbidden());
        verifyNoInteractions(vocabService);
    }

    @Test
    void nullWordIsABadRequestNotAServerError() throws Exception {
        mvc.perform(post("/v1/vocab/words").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content("{\"words\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("words must not contain null"));
    }

    @Test
    void hugePageIsRejected() throws Exception {
        mvc.perform(get("/v1/vocab/words").param("page", "50000000")).andExpect(status().isBadRequest());
        mvc.perform(get("/v1/vocab/words").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void concurrentDuplicateBecomesConflict() throws Exception {
        doThrow(new DataIntegrityViolationException("Duplicate entry"))
                .when(vocabService).saveWord(eq(7L), eq(3L));
        mvc.perform(put("/v1/vocab/saved/3").with(USER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void creatingUsersIsAdminOnlyAndValidated() throws Exception {
        String body = "{\"username\":\"someone\",\"password\":\"long-enough\",\"role\":null}";
        mvc.perform(post("/v1/admin/users").with(USER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/admin/users").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("role must be USER or ADMIN"));
        verifyNoInteractions(authService);
    }

    @Test
    void loginUsesTheLastForwardedHop() throws Exception {
        mvc.perform(post("/v1/auth/login")
                        .header("X-Forwarded-For", "10.0.0.1, 203.0.113.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());
        verify(authService).login(any(), eq("203.0.113.7"));
    }

    @Test
    void meNeedsAToken() throws Exception {
        mvc.perform(get("/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejectedWithTheSharedErrorShape() throws Exception {
        mvc.perform(get("/v1/vocab/saved").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void corsPreflightAllowsTheSiteToSendTokens() throws Exception {
        mvc.perform(options("/v1/vocab/saved/1")
                        .header("Origin", "https://nttuan.dev")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://nttuan.dev"));
        verify(vocabService, org.mockito.Mockito.never()).saveWord(any(), isNull());
    }
}

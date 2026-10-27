package com.tuannt.api.web;

import com.tuannt.api.configs.AuthConfig;
import com.tuannt.api.configs.CorsConfig;
import com.tuannt.api.configs.GlobalConfig;
import com.tuannt.api.configs.SecurityConfig;
import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.constants.UserRole;
import com.tuannt.api.controllers.AdminContactController;
import com.tuannt.api.controllers.AdminController;
import com.tuannt.api.controllers.AdminPostController;
import com.tuannt.api.controllers.AdminProjectController;
import com.tuannt.api.controllers.AuthController;
import com.tuannt.api.controllers.PostController;
import com.tuannt.api.controllers.ProjectController;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.services.AuthService;
import com.tuannt.api.services.ContactService;
import com.tuannt.api.services.PostService;
import com.tuannt.api.services.ProjectService;
import com.tuannt.api.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access rules and validation for the blog, portfolio, inbox and user-management routes.
 * Services are mocked, so this runs without a database.
 */
@WebMvcTest(controllers = {PostController.class, AdminPostController.class, ProjectController.class,
        AdminProjectController.class, AdminContactController.class, AdminController.class, AuthController.class})
@Import({SecurityConfig.class, GlobalConfig.class, CommonUtil.class, ContentAdminApiTest.Properties.class})
@TestPropertySource(properties = {
        "auth.jwtSecret=test-secret-that-is-at-least-32-bytes-long",
        "cors.allowedOriginPatterns=https://nttuan.dev",
})
class ContentAdminApiTest {
    private static final RequestPostProcessor ADMIN =
            jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final RequestPostProcessor USER =
            jwt().jwt(j -> j.subject("7")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    private static final String POST_BODY = "{\"title\":\"Xin chào\",\"content\":\"# Hi\",\"tags\":[\"java\"]}";
    private static final String PROJECT_BODY = "{\"name\":\"Site\",\"summary\":\"My site\",\"tech\":[\"React\"]}";

    @TestConfiguration
    @EnableConfigurationProperties({AuthConfig.class, CorsConfig.class})
    static class Properties {
    }

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private PostService postService;
    @MockitoBean
    private ProjectService projectService;
    @MockitoBean
    private ContactService contactService;
    @MockitoBean
    private AuthService authService;

    @Test
    void publicReadsNeedNoToken() throws Exception {
        when(postService.listPublished(anyInt(), anyInt(), any())).thenReturn(PageResDto.<com.tuannt.api.dtos.post.PostSummaryDto>builder().items(List.of()).build());
        mvc.perform(get("/v1/posts").param("tag", "java")).andExpect(status().isOk());
        verify(postService).listPublished(0, 20, "java");
        mvc.perform(get("/v1/posts/tags")).andExpect(status().isOk());
        verify(postService).publishedTags();
        mvc.perform(get("/v1/projects")).andExpect(status().isOk());
        verify(projectService).listVisible();
    }

    @Test
    void unpublishedPostIsA404WithTheSharedShape() throws Exception {
        when(postService.getPublished("draft")).thenThrow(new NotFoundException(ApiStatus.POST_NOT_FOUND));
        mvc.perform(get("/v1/posts/draft"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("POST_NOT_FOUND"));
    }

    @Test
    void everyAdminRouteRejectsAnonymousAndUsers() throws Exception {
        List<RequestBuilder> anonymous = List.of(
                get("/v1/admin/posts"), get("/v1/admin/posts/1"), json(post("/v1/admin/posts"), POST_BODY),
                json(put("/v1/admin/posts/1"), POST_BODY), delete("/v1/admin/posts/1"),
                get("/v1/admin/projects"), json(post("/v1/admin/projects"), PROJECT_BODY), delete("/v1/admin/projects/1"),
                get("/v1/admin/contact-messages"), put("/v1/admin/contact-messages/1/read"),
                delete("/v1/admin/contact-messages/1"),
                get("/v1/admin/users"), json(put("/v1/admin/users/2/role"), "{\"role\":\"USER\"}"),
                json(put("/v1/admin/users/2/password"), "{\"password\":\"long-enough\"}"), delete("/v1/admin/users/2"));
        for (RequestBuilder request : anonymous) {
            mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        }
        mvc.perform(get("/v1/admin/posts").with(USER)).andExpect(status().isForbidden());
        mvc.perform(json(post("/v1/admin/projects"), PROJECT_BODY).with(USER)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/admin/contact-messages").with(USER)).andExpect(status().isForbidden());
        mvc.perform(delete("/v1/admin/users/2").with(USER)).andExpect(status().isForbidden());
        verifyNoInteractions(postService, projectService, contactService, authService);
    }

    @Test
    void adminCanUseEveryRouteGroup() throws Exception {
        mvc.perform(get("/v1/admin/posts").param("status", "DRAFT").with(ADMIN)).andExpect(status().isOk());
        verify(postService).listAll(0, 20, PostStatus.DRAFT);
        mvc.perform(json(post("/v1/admin/posts"), POST_BODY).with(ADMIN)).andExpect(status().isCreated());
        mvc.perform(json(post("/v1/admin/projects"), PROJECT_BODY).with(ADMIN)).andExpect(status().isCreated());
        mvc.perform(get("/v1/admin/contact-messages").param("unread", "true").with(ADMIN)).andExpect(status().isOk());
        verify(contactService).list(0, 20, true);
        mvc.perform(put("/v1/admin/contact-messages/4/read").with(ADMIN)).andExpect(status().isOk());
        verify(contactService).markRead(4L);
        mvc.perform(delete("/v1/admin/contact-messages/4").with(ADMIN)).andExpect(status().isNoContent());
        verify(contactService).delete(4L);
        mvc.perform(get("/v1/admin/users").with(ADMIN)).andExpect(status().isOk());
    }

    @Test
    void invalidPostInputIsABadRequest() throws Exception {
        mvc.perform(json(post("/v1/admin/posts"), "{\"slug\":\"Not A Slug\",\"title\":\"t\",\"content\":\"c\",\"tags\":[]}").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("slug must be lowercase letters, digits and single dashes, like my-first-post"));
        mvc.perform(json(post("/v1/admin/posts"), "{\"title\":\"t\",\"content\":\"c\",\"coverUrl\":\"javascript:alert(1)\"}").with(ADMIN))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/admin/posts").param("status", "ARCHIVED").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
        verifyNoInteractions(postService);
    }

    @Test
    void slugConflictKeepsItsErrorCode() throws Exception {
        when(postService.create(any())).thenThrow(new BadRequestException(ApiStatus.SLUG_TAKEN, "Slug already exists: xin-chao"));
        mvc.perform(json(post("/v1/admin/posts"), POST_BODY).with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SLUG_TAKEN"))
                .andExpect(jsonPath("$.errorMessage").value("Slug already exists: xin-chao"));
    }

    @Test
    void roleAndDeleteUseTheTokenSubjectAsActor() throws Exception {
        mvc.perform(json(put("/v1/admin/users/2/role"), "{\"role\":\"USER\"}").with(ADMIN)).andExpect(status().isOk());
        verify(authService).setRole(1L, 2L, UserRole.USER);
        mvc.perform(delete("/v1/admin/users/2").with(ADMIN)).andExpect(status().isNoContent());
        verify(authService).deleteUser(1L, 2L);
        mvc.perform(json(put("/v1/admin/users/2/role"), "{\"role\":null}").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("role must be USER or ADMIN"));
    }

    @Test
    void adminPasswordResetIsValidated() throws Exception {
        mvc.perform(json(put("/v1/admin/users/2/password"), "{\"password\":\"short\"}").with(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("password must be 8-100 characters"));
        mvc.perform(json(put("/v1/admin/users/2/password"), "{\"password\":\"long-enough\"}").with(ADMIN))
                .andExpect(status().isNoContent());
        verify(authService).setPassword(2L, "long-enough");
    }

    @Test
    void ownPasswordChangeNeedsLogin() throws Exception {
        String body = "{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\"}";
        mvc.perform(json(put("/v1/auth/password"), body)).andExpect(status().isUnauthorized());
        verifyNoInteractions(authService);
        mvc.perform(json(put("/v1/auth/password"), body).with(USER)).andExpect(status().isNoContent());
        verify(authService).changeOwnPassword(eq(7L), any());
        mvc.perform(json(put("/v1/auth/password"), "{\"currentPassword\":\"x\",\"newPassword\":\"short\"}").with(USER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicPostListValidatesPaging() throws Exception {
        mvc.perform(get("/v1/posts").param("size", "101")).andExpect(status().isBadRequest());
        verify(postService, org.mockito.Mockito.never()).listPublished(anyInt(), anyInt(), isNull());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder json(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}

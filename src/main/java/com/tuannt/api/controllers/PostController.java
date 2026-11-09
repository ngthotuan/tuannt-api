package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.post.PostDto;
import com.tuannt.api.dtos.post.PostSummaryDto;
import com.tuannt.api.dtos.post.TagCountDto;
import com.tuannt.api.services.PostService;
import com.tuannt.api.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Public blog reads. Only PUBLISHED posts are ever returned here; drafts live under
 * /v1/admin/posts.
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_POST_PATH)
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final CommonUtil commonUtil;

    @GetMapping
    public PageResDto<PostSummaryDto> list(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be at least 0")
            @Max(value = 100_000, message = "page must be at most 100000") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(required = false) @Size(max = 40, message = "tag must be at most 40 characters") String tag,
            @RequestParam(required = false) @Size(max = 100, message = "q must be at most 100 characters") String q) {
        return postService.listPublished(page, size, tag, q);
    }

    /** Mapped before /{slug}, which is why "tags" can never be a post slug. */
    @GetMapping("/tags")
    public List<TagCountDto> tags() {
        return postService.publishedTags();
    }

    /**
     * A logged-in caller (any role) gets MEMBERS bodies; {@code preview} is the secret from an
     * admin-issued preview link.
     */
    @GetMapping("/{slug}")
    public PostDto get(@PathVariable String slug,
                       @RequestParam(required = false) @Size(max = 64, message = "preview must be at most 64 characters") String preview,
                       @AuthenticationPrincipal Jwt jwt) {
        return postService.getPublished(slug, jwt != null, preview);
    }

    @GetMapping("/{slug}/related")
    public List<PostSummaryDto> related(@PathVariable String slug,
                                        @RequestParam(defaultValue = "3")
                                        @Min(value = 1, message = "limit must be at least 1")
                                        @Max(value = 10, message = "limit must be at most 10") int limit) {
        return postService.related(slug, limit);
    }

    /** Fire-and-forget read counter; always 204 so it reveals nothing about the slug. */
    @PostMapping("/{slug}/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void view(@PathVariable String slug, HttpServletRequest request) {
        postService.recordView(slug, commonUtil.clientIp(request));
    }
}

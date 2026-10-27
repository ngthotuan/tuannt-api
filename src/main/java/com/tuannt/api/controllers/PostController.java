package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.post.PostDto;
import com.tuannt.api.dtos.post.PostSummaryDto;
import com.tuannt.api.dtos.post.TagCountDto;
import com.tuannt.api.services.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public PageResDto<PostSummaryDto> list(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be at least 0")
            @Max(value = 100_000, message = "page must be at most 100000") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(required = false) @Size(max = 40, message = "tag must be at most 40 characters") String tag) {
        return postService.listPublished(page, size, tag);
    }

    /** Mapped before /{slug}, which is why "tags" can never be a post slug. */
    @GetMapping("/tags")
    public List<TagCountDto> tags() {
        return postService.publishedTags();
    }

    @GetMapping("/{slug}")
    public PostDto get(@PathVariable String slug) {
        return postService.getPublished(slug);
    }
}

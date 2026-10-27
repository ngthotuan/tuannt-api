package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.post.PostDto;
import com.tuannt.api.dtos.post.PostReqDto;
import com.tuannt.api.dtos.post.PostSummaryDto;
import com.tuannt.api.services.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * ADMIN only (enforced in SecurityConfig). Publishing and unpublishing is just the status field.
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_ADMIN_PATH + "/posts")
@RequiredArgsConstructor
public class AdminPostController {
    private final PostService postService;

    @GetMapping
    public PageResDto<PostSummaryDto> list(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be at least 0")
            @Max(value = 100_000, message = "page must be at most 100000") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(required = false) PostStatus status) {
        return postService.listAll(page, size, status);
    }

    @GetMapping("/{id}")
    public PostDto get(@PathVariable Long id) {
        return postService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto create(@Valid @RequestBody PostReqDto req) {
        return postService.create(req);
    }

    @PutMapping("/{id}")
    public PostDto update(@PathVariable Long id, @Valid @RequestBody PostReqDto req) {
        return postService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        postService.delete(id);
    }
}

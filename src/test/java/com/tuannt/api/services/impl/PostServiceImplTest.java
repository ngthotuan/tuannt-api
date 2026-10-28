package com.tuannt.api.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.dtos.post.PostDto;
import com.tuannt.api.dtos.post.PostReqDto;
import com.tuannt.api.entities.Post;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.repositories.PostRepository;
import com.tuannt.api.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostServiceImplTest {
    private final PostRepository repository = mock(PostRepository.class);
    private final PostServiceImpl service = new PostServiceImpl(repository, new CommonUtil(new ObjectMapper()));

    @BeforeEach
    void saveAssignsAnId() {
        when(repository.save(any())).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(42L);
            return post;
        });
    }

    @Test
    void slugIsDerivedFromAVietnameseTitle() {
        PostDto created = service.create(req(null, "Học Spring Boot ở Đà Nẵng", PostStatus.DRAFT));

        assertThat(created.getSlug()).isEqualTo("hoc-spring-boot-o-da-nang");
        assertThat(created.getTags()).containsExactly("spring-boot", "java");
        assertThat(created.getPublishedAt()).isNull();
    }

    @Test
    void takenOrReservedSlugIsABadRequest() {
        when(repository.existsBySlug("taken")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("taken", "Title", PostStatus.DRAFT)))
                .isInstanceOf(BadRequestException.class)
                .extracting(e -> ((BadRequestException) e).getApiError().getErrorCode()).isEqualTo("SLUG_TAKEN");
        assertThatThrownBy(() -> service.create(req("tags", "Title", PostStatus.DRAFT)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(req(null, "!!!", PostStatus.DRAFT)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void publishingSetsThePublishedDate() {
        PostDto created = service.create(req("p", "Title", PostStatus.PUBLISHED));
        assertThat(created.getPublishedAt()).isNotNull();
    }

    @Test
    void editingKeepsTheSlugAndUnpublishingKeepsTheFirstPublishedDate() {
        Post draft = existing(PostStatus.DRAFT, null);
        when(repository.findById(1L)).thenReturn(Optional.of(draft));
        service.update(1L, req("", "Edited", PostStatus.DRAFT));
        assertThat(draft.getSlug()).isEqualTo("existing");

        Instant firstPublished = Instant.parse("2026-01-01T00:00:00Z");
        Post published = existing(PostStatus.PUBLISHED, firstPublished);
        when(repository.findById(2L)).thenReturn(Optional.of(published));
        service.update(2L, req(null, "Hidden again", PostStatus.DRAFT));
        assertThat(published.getPublishedAt()).isEqualTo(firstPublished);
    }

    @Test
    void unpublishedPostIsNotFoundPublicly() {
        when(repository.findBySlug("draft")).thenReturn(Optional.of(existing(PostStatus.DRAFT, null)));
        assertThatThrownBy(() -> service.getPublished("draft"))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getApiError().getErrorCode()).isEqualTo("POST_NOT_FOUND");
        assertThatThrownBy(() -> service.getPublished("missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void tagCountsAreMostUsedFirst() {
        when(repository.findTagsByStatus(PostStatus.PUBLISHED))
                .thenReturn(List.of("[\"java\",\"spring\"]", "[\"java\"]", "[]"));
        assertThat(service.publishedTags())
                .extracting(t -> t.getTag() + "=" + t.getCount())
                .containsExactly("java=2", "spring=1");
    }

    @Test
    void contentIsAbbreviatedInLogs() {
        PostReqDto req = req("p", "Title", PostStatus.DRAFT);
        req.setContent("x".repeat(10_000));
        assertThat(req.toString()).contains("<10000 chars>").doesNotContain("xxxxxxxxxx");
    }

    private static PostReqDto req(String slug, String title, PostStatus status) {
        PostReqDto req = new PostReqDto();
        req.setSlug(slug);
        req.setTitle(title);
        req.setContent("# Hello");
        req.setTags(List.of("Spring Boot", "java", "JAVA"));
        req.setStatus(status);
        return req;
    }

    private static Post existing(PostStatus status, Instant publishedAt) {
        Post post = new Post();
        post.setId(1L);
        post.setSlug("existing");
        post.setTitle("Existing");
        post.setContent("body");
        post.setTags("[]");
        post.setStatus(status);
        post.setPublishedAt(publishedAt);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        return post;
    }
}

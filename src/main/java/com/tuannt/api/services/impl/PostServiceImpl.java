package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.constants.ValidationPatterns;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.post.PostDto;
import com.tuannt.api.dtos.post.PostReqDto;
import com.tuannt.api.dtos.post.PostSummaryDto;
import com.tuannt.api.dtos.post.TagCountDto;
import com.tuannt.api.entities.Post;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.repositories.PostRepository;
import com.tuannt.api.repositories.PostSummaryView;
import com.tuannt.api.services.DeployHookService;
import com.tuannt.api.services.PostService;
import com.tuannt.api.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private static final int TAG_MAX_LENGTH = 40;
    /** Literal sub-paths of /v1/posts that a post slug would otherwise shadow. */
    private static final Set<String> RESERVED_SLUGS = Set.of("tags");

    private final PostRepository postRepository;
    private final DeployHookService deployHookService;
    private final CommonUtil commonUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResDto<PostSummaryDto> listPublished(int page, int size, String tag) {
        // id breaks ties so paging is stable when two posts share a publish time.
        Sort sort = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
        String normalizedTag = commonUtil.slugify(tag, TAG_MAX_LENGTH);
        String tagPattern = normalizedTag.isEmpty() ? null : "%\"" + normalizedTag + "\"%";
        return PageResDto.of(postRepository.findSummaries(PostStatus.PUBLISHED, tagPattern, PageRequest.of(page, size, sort)),
                this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getPublished(String slug) {
        return postRepository.findBySlug(slug)
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException(ApiStatus.POST_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagCountDto> publishedTags() {
        // Counted in memory: a personal blog has tens of posts, and tags live in a JSON column.
        Map<String, Long> counts = new HashMap<>();
        for (String json : postRepository.findTagsByStatus(PostStatus.PUBLISHED)) {
            for (String tag : commonUtil.fromJsonArray(json)) {
                counts.merge(tag, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(e -> TagCountDto.builder().tag(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResDto<PostSummaryDto> listAll(int page, int size, PostStatus status) {
        Sort sort = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        return PageResDto.of(postRepository.findSummaries(status, null, PageRequest.of(page, size, sort)),
                this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto get(Long id) {
        return toDto(findPost(id));
    }

    @Override
    @Transactional
    public PostDto create(PostReqDto req) {
        String slug = StringUtils.hasText(req.getSlug()) ? req.getSlug() : deriveSlug(req.getTitle());
        checkSlug(slug, postRepository.existsBySlug(slug));

        Instant now = Instant.now();
        Post post = new Post();
        post.setSlug(slug);
        post.setCreatedAt(now);
        apply(post, req, now);
        Post saved = postRepository.save(post);
        log.info("Post created id: {} slug: {} status: {}", saved.getId(), saved.getSlug(), saved.getStatus());
        if (saved.getStatus() == PostStatus.PUBLISHED) {
            deployHookService.contentChanged("post published: " + saved.getSlug());
        }
        return toDto(saved);
    }

    @Override
    @Transactional
    public PostDto update(Long id, PostReqDto req) {
        Post post = findPost(id);
        boolean wasPublished = post.getStatus() == PostStatus.PUBLISHED;
        String oldSlug = post.getSlug();
        // An empty slug keeps the current one: re-deriving it from an edited title would
        // silently break every link to the post.
        if (StringUtils.hasText(req.getSlug()) && !req.getSlug().equals(post.getSlug())) {
            checkSlug(req.getSlug(), postRepository.existsBySlugAndIdNot(req.getSlug(), id));
            post.setSlug(req.getSlug());
        }
        apply(post, req, Instant.now());
        log.info("Post updated id: {} slug: {} status: {}", id, post.getSlug(), post.getStatus());
        if (wasPublished || post.getStatus() == PostStatus.PUBLISHED) {
            deployHookService.contentChanged("post updated: " + oldSlug);
        }
        return toDto(post);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Post post = findPost(id);
        postRepository.delete(post);
        log.info("Post deleted id: {} slug: {}", id, post.getSlug());
        if (post.getStatus() == PostStatus.PUBLISHED) {
            deployHookService.contentChanged("post deleted: " + post.getSlug());
        }
    }

    private void apply(Post post, PostReqDto req, Instant now) {
        post.setTitle(req.getTitle().trim());
        post.setExcerpt(blankToNull(req.getExcerpt()));
        post.setContent(req.getContent());
        post.setCoverUrl(blankToNull(req.getCoverUrl()));
        post.setTags(commonUtil.toJsonArray(normalizeTags(req.getTags())));
        post.setStatus(req.getStatus());
        if (req.getStatus() == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }
        post.setUpdatedAt(now);
    }

    private String deriveSlug(String title) {
        String slug = commonUtil.slugify(title, ValidationPatterns.SLUG_MAX_LENGTH);
        if (slug.isEmpty()) {
            throw new BadRequestException("slug could not be derived from the title, please provide one");
        }
        return slug;
    }

    private void checkSlug(String slug, boolean taken) {
        if (RESERVED_SLUGS.contains(slug)) {
            throw new BadRequestException("slug is reserved: " + slug);
        }
        if (taken) {
            throw new BadRequestException(ApiStatus.SLUG_TAKEN, "Slug already exists: " + slug);
        }
    }

    /** Kebab-cased, de-duplicated, order kept; "Spring Boot" and "spring-boot" are the same tag. */
    private List<String> normalizeTags(List<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalized = commonUtil.slugify(tag, TAG_MAX_LENGTH);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private Post findPost(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new NotFoundException(ApiStatus.POST_NOT_FOUND));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Long millis(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    private PostSummaryDto toSummaryDto(PostSummaryView p) {
        return PostSummaryDto.builder()
                .id(p.id())
                .slug(p.slug())
                .title(p.title())
                .excerpt(p.excerpt())
                .coverUrl(p.coverUrl())
                .tags(commonUtil.fromJsonArray(p.tags()))
                .status(p.status())
                .publishedAt(millis(p.publishedAt()))
                .createdAt(millis(p.createdAt()))
                .updatedAt(millis(p.updatedAt()))
                .build();
    }

    private PostDto toDto(Post p) {
        return PostDto.builder()
                .id(p.getId())
                .slug(p.getSlug())
                .title(p.getTitle())
                .excerpt(p.getExcerpt())
                .content(p.getContent())
                .coverUrl(p.getCoverUrl())
                .tags(commonUtil.fromJsonArray(p.getTags()))
                .status(p.getStatus())
                .publishedAt(millis(p.getPublishedAt()))
                .createdAt(millis(p.getCreatedAt()))
                .updatedAt(millis(p.getUpdatedAt()))
                .build();
    }
}

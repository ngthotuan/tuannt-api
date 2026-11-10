package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.constants.PostVisibility;
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
import com.tuannt.api.services.PostService;
import com.tuannt.api.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
    private static final int QUERY_MAX_LENGTH = 100;
    /** Reads from one address count once per post in this window. */
    private static final Duration VIEW_WINDOW = Duration.ofHours(6);
    /** Related posts are picked in memory from this many recent ones: plenty for a personal blog. */
    private static final int RELATED_POOL = 200;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PostRepository postRepository;
    private final CommonUtil commonUtil;
    /** Its own instance, so view dedupe keys never crowd out the contact form's limits. */
    private final RateLimiter viewDedupe = new RateLimiter();

    @Override
    @Transactional(readOnly = true)
    public PageResDto<PostSummaryDto> listPublished(int page, int size, String tag, String query) {
        String normalizedTag = commonUtil.slugify(tag, TAG_MAX_LENGTH);
        String tagPattern = normalizedTag.isEmpty() ? null : "%\"" + normalizedTag + "\"%";
        // id breaks ties so paging is stable when two posts share a publish time.
        return PageResDto.of(postRepository.findSummaries(PostStatus.PUBLISHED, Instant.now(), PostVisibility.UNLISTED,
                        tagPattern, likePattern(query), PageRequest.of(page, size, NEWEST_FIRST)),
                this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getPublished(String slug, boolean member, String previewKey) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException(ApiStatus.POST_NOT_FOUND));
        if (matchesPreviewKey(post, previewKey)) {
            return toDto(post, true, false);
        }
        if (!isLive(post, Instant.now())) {
            throw new NotFoundException(ApiStatus.POST_NOT_FOUND);
        }
        return toDto(post, member || post.getVisibility() != PostVisibility.MEMBERS, false);
    }

    @Override
    @Transactional
    public void recordView(String slug, String clientIp) {
        postRepository.findBySlug(slug)
                .filter(post -> isLive(post, Instant.now()))
                .filter(post -> viewDedupe.tryAcquire(clientIp + "|" + post.getId(), 1, VIEW_WINDOW))
                .ifPresent(post -> postRepository.incrementViewCount(post.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostSummaryDto> related(String slug, int limit) {
        Post post = postRepository.findBySlug(slug)
                .filter(p -> isLive(p, Instant.now()))
                .orElseThrow(() -> new NotFoundException(ApiStatus.POST_NOT_FOUND));
        Set<String> tags = Set.copyOf(commonUtil.fromJsonArray(post.getTags()));
        if (tags.isEmpty()) {
            return List.of();
        }
        List<PostSummaryView> candidates = postRepository.findSummaries(PostStatus.PUBLISHED, Instant.now(),
                PostVisibility.UNLISTED, null, null, PageRequest.of(0, RELATED_POOL, NEWEST_FIRST)).getContent();
        // Candidates arrive newest first and the sort is stable, so ties stay newest first.
        return candidates.stream()
                .filter(c -> !c.id().equals(post.getId()))
                .map(c -> Map.entry(c, commonUtil.fromJsonArray(c.tags()).stream().filter(tags::contains).count()))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<PostSummaryView, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> toSummaryDto(e.getKey()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagCountDto> publishedTags() {
        // Counted in memory: a personal blog has tens of posts, and tags live in a JSON column.
        Map<String, Long> counts = new HashMap<>();
        for (String json : postRepository.findListedTags(PostStatus.PUBLISHED, Instant.now(), PostVisibility.UNLISTED)) {
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
        return PageResDto.of(postRepository.findSummaries(status, null, null, null, null, PageRequest.of(page, size, sort)),
                this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto get(Long id) {
        return toDto(findPost(id), true, true);
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
        return toDto(saved, true, true);
    }

    @Override
    @Transactional
    public PostDto update(Long id, PostReqDto req) {
        Post post = findPost(id);
        // An empty slug keeps the current one: re-deriving it from an edited title would
        // silently break every link to the post.
        if (StringUtils.hasText(req.getSlug()) && !req.getSlug().equals(post.getSlug())) {
            checkSlug(req.getSlug(), postRepository.existsBySlugAndIdNot(req.getSlug(), id));
            post.setSlug(req.getSlug());
        }
        apply(post, req, Instant.now());
        log.info("Post updated id: {} slug: {} status: {}", id, post.getSlug(), post.getStatus());
        return toDto(post, true, true);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Post post = findPost(id);
        postRepository.delete(post);
        log.info("Post deleted id: {} slug: {}", id, post.getSlug());
    }

    @Override
    @Transactional
    public PostDto createPreviewKey(Long id) {
        Post post = findPost(id);
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        post.setPreviewKey(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        log.info("Post preview link issued id: {}", id);
        return toDto(post, true, true);
    }

    @Override
    @Transactional
    public void revokePreviewKey(Long id) {
        findPost(id).setPreviewKey(null);
        log.info("Post preview link revoked id: {}", id);
    }

    /** Published and due: what anyone may read by slug (MEMBERS bodies aside). */
    private static boolean isLive(Post post, Instant now) {
        return post.getStatus() == PostStatus.PUBLISHED
                && post.getPublishedAt() != null && !post.getPublishedAt().isAfter(now);
    }

    private static boolean matchesPreviewKey(Post post, String previewKey) {
        return StringUtils.hasText(previewKey) && post.getPreviewKey() != null
                && MessageDigest.isEqual(post.getPreviewKey().getBytes(StandardCharsets.UTF_8),
                previewKey.getBytes(StandardCharsets.UTF_8));
    }

    /** Lowercase substring pattern with LIKE wildcards escaped by '!'; null when there is no query. */
    private static String likePattern(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String trimmed = query.trim().toLowerCase(Locale.ROOT);
        if (trimmed.length() > QUERY_MAX_LENGTH) {
            trimmed = trimmed.substring(0, QUERY_MAX_LENGTH);
        }
        return "%" + trimmed.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private void apply(Post post, PostReqDto req, Instant now) {
        post.setTitle(req.getTitle().trim());
        post.setExcerpt(blankToNull(req.getExcerpt()));
        post.setContent(req.getContent());
        post.setCoverUrl(blankToNull(req.getCoverUrl()));
        post.setTags(commonUtil.toJsonArray(normalizeTags(req.getTags())));
        post.setStatus(req.getStatus());
        post.setVisibility(req.getVisibility());
        // Whole seconds: DATETIME rounds fractions to the nearest second, so 10:00:00.6 would be
        // stored as 10:00:01 and a post published "now" would read as scheduled for a moment.
        if (req.getPublishedAt() != null) {
            post.setPublishedAt(Instant.ofEpochMilli(req.getPublishedAt()).truncatedTo(ChronoUnit.SECONDS));
        } else if (req.getStatus() == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(now.truncatedTo(ChronoUnit.SECONDS));
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
                .visibility(p.visibility())
                .viewCount(p.viewCount())
                .publishedAt(millis(p.publishedAt()))
                .createdAt(millis(p.createdAt()))
                .updatedAt(millis(p.updatedAt()))
                .build();
    }

    /**
     * @param withContent false withholds the body (a MEMBERS post read anonymously)
     * @param admin       true adds the preview secret
     */
    private PostDto toDto(Post p, boolean withContent, boolean admin) {
        return PostDto.builder()
                .id(p.getId())
                .slug(p.getSlug())
                .title(p.getTitle())
                .excerpt(p.getExcerpt())
                .content(withContent ? p.getContent() : null)
                .coverUrl(p.getCoverUrl())
                .tags(commonUtil.fromJsonArray(p.getTags()))
                .status(p.getStatus())
                .visibility(p.getVisibility())
                .viewCount(p.getViewCount())
                .publishedAt(millis(p.getPublishedAt()))
                .createdAt(millis(p.getCreatedAt()))
                .updatedAt(millis(p.getUpdatedAt()))
                .locked(!withContent)
                .previewKey(admin ? p.getPreviewKey() : null)
                .build();
    }
}

package com.tuannt.api.repositories;

import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.constants.PostVisibility;
import com.tuannt.api.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // Every filter is optional (null = no filter). :tagPattern is '%"tag"%': tags are a JSON array
    // of [a-z0-9-] values, so the quotes make it an exact element match with nothing to escape.
    // :query is a lowercase LIKE pattern whose wildcards the caller escaped with '!'.
    @Query(value = """
            select new com.tuannt.api.repositories.PostSummaryView(p.id, p.slug, p.title, p.excerpt,
                p.coverUrl, p.tags, p.status, p.visibility, p.viewCount, p.publishedAt, p.createdAt, p.updatedAt)
            from Post p
            where (:status is null or p.status = :status)
              and (:publishedBefore is null or p.publishedAt <= :publishedBefore)
              and (:excludedVisibility is null or p.visibility <> :excludedVisibility)
              and (:tagPattern is null or p.tags like :tagPattern)
              and (:query is null or lower(p.title) like :query escape '!'
                   or lower(coalesce(p.excerpt, '')) like :query escape '!')
            """,
            countQuery = """
                    select count(p) from Post p
                    where (:status is null or p.status = :status)
                      and (:publishedBefore is null or p.publishedAt <= :publishedBefore)
                      and (:excludedVisibility is null or p.visibility <> :excludedVisibility)
                      and (:tagPattern is null or p.tags like :tagPattern)
                      and (:query is null or lower(p.title) like :query escape '!'
                           or lower(coalesce(p.excerpt, '')) like :query escape '!')
                    """)
    Page<PostSummaryView> findSummaries(PostStatus status, Instant publishedBefore, PostVisibility excludedVisibility,
                                        String tagPattern, String query, Pageable pageable);

    /** Tags of the posts the public list shows: published, already due, not unlisted. */
    @Query("""
            select p.tags from Post p
            where p.status = :status and p.publishedAt <= :publishedBefore and p.visibility <> :excludedVisibility
            """)
    List<String> findListedTags(PostStatus status, Instant publishedBefore, PostVisibility excludedVisibility);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int incrementViewCount(Long id);
}

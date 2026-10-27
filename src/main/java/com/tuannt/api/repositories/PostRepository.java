package com.tuannt.api.repositories;

import com.tuannt.api.constants.PostStatus;
import com.tuannt.api.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

    // :status null means any status. :tagPattern is null or '%"tag"%': tags are a JSON array of
    // [a-z0-9-] values, so the quotes make it an exact element match with nothing to escape.
    @Query(value = """
            select new com.tuannt.api.repositories.PostSummaryView(p.id, p.slug, p.title, p.excerpt,
                p.coverUrl, p.tags, p.status, p.publishedAt, p.createdAt, p.updatedAt)
            from Post p
            where (:status is null or p.status = :status)
              and (:tagPattern is null or p.tags like :tagPattern)
            """,
            countQuery = """
                    select count(p) from Post p
                    where (:status is null or p.status = :status)
                      and (:tagPattern is null or p.tags like :tagPattern)
                    """)
    Page<PostSummaryView> findSummaries(PostStatus status, String tagPattern, Pageable pageable);

    @Query("select p.tags from Post p where p.status = :status")
    List<String> findTagsByStatus(PostStatus status);
}

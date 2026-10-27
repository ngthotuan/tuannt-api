package com.tuannt.api.entities;

import com.tuannt.api.constants.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * A blog post written in Markdown. The static site renders it; this API only stores it.
 */

@Getter
@Setter
@Entity
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String excerpt;

    /** Markdown, MEDIUMTEXT. */
    @Column(nullable = false)
    private String content;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    // Raw JSON array text rather than a converted List: a converter would also be applied to the
    // String bound in the tag LIKE filter and fail.
    @Column(nullable = false, length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostStatus status;

    /** Set on first publish and kept through unpublish/republish, so the post keeps its date. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

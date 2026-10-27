package com.tuannt.api.repositories;

import com.tuannt.api.constants.PostStatus;

import java.time.Instant;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * A post without its Markdown body, so list pages never pull whole documents from the database.
 */
public record PostSummaryView(Long id, String slug, String title, String excerpt, String coverUrl,
                              String tags, PostStatus status, Instant publishedAt, Instant createdAt,
                              Instant updatedAt) {
}

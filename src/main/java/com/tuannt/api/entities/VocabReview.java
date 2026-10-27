package com.tuannt.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A user's spaced-repetition state for one word. Plain ids like VocabSavedWord: there are no
 * foreign keys, so deleting a word or a user removes these rows explicitly in the services.
 */

@Getter
@Setter
@Entity
@Table(name = "vocab_review")
public class VocabReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(nullable = false)
    private double ease;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(nullable = false)
    private int repetitions;

    @Column(nullable = false)
    private int lapses;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "last_reviewed_at", nullable = false)
    private Instant lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

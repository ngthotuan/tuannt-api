package com.tuannt.api.entities;

import com.tuannt.api.constants.ReviewGrade;
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
import java.time.LocalDate;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * One answered review. Append-only history for the stats, since VocabReview keeps only the latest.
 */

@Getter
@Setter
@Entity
@Table(name = "vocab_review_log")
public class VocabReviewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private ReviewGrade grade;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    /** UTC date of reviewedAt, stored so the streak query needs no time-zone math in SQL. */
    @Column(name = "review_day", nullable = false)
    private LocalDate reviewDay;
}

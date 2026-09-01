package com.tuannt.api.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Getter
@Setter
@Entity
@Table(name = "quiz_attempt")
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Anonymous per-browser id from localStorage. No account system exists. */
    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(nullable = false, length = 16)
    private String mode;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Instant, not LocalDateTime: the value is stored and returned as an unambiguous UTC
    // point in time, matching the epoch-millis the rest of the API already returns.
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // nullable = false makes Hibernate write attempt_id in the child INSERT itself. Without it
    // the child is inserted with a null FK and patched by a later UPDATE, which a NOT NULL
    // column rejects outright.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "attempt_id", nullable = false)
    private List<QuizAttemptAnswer> wrongAnswers = new ArrayList<>();
}

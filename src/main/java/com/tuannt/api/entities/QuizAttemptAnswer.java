package com.tuannt.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by tuannt7 on 01/09/2026
 * <p>
 * Only wrong answers are stored: enough to review mistakes without copying whole question banks
 * into the database.
 */

@Getter
@Setter
@Entity
@Table(name = "quiz_attempt_answer")
public class QuizAttemptAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_index", nullable = false)
    private int questionIndex;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(name = "selected_text", length = 500)
    private String selectedText;

    @Column(name = "correct_text", length = 500)
    private String correctText;
}

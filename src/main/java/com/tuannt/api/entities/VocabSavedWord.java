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
 * Created by tuannt7 on 21/10/2026
 * <p>
 * A word a user bookmarked to study. Plain ids rather than JPA relations: the API only ever
 * needs the id list, and the schema has no foreign keys, so deleting a word removes these rows
 * explicitly in the service.
 */

@Getter
@Setter
@Entity
@Table(name = "vocab_saved_word")
public class VocabSavedWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

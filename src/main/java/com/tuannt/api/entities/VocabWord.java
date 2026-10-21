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
 * One shared word list, managed by admins. Each English term appears once; several meanings go
 * into the same {@code vi} text.
 */

@Getter
@Setter
@Entity
@Table(name = "vocab_word")
public class VocabWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String en;

    @Column(nullable = false, length = 500)
    private String vi;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

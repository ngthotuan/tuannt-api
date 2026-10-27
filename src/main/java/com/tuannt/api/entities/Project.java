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
 * A portfolio entry. Hidden ones (visible = false) are only listed to admins.
 */

@Getter
@Setter
@Entity
@Table(name = "project")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 300)
    private String summary;

    /** Markdown, MEDIUMTEXT. */
    private String description;

    /** JSON array of technology names. */
    @Column(nullable = false, length = 1500)
    private String tech;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(name = "demo_url", length = 500)
    private String demoUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean visible;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

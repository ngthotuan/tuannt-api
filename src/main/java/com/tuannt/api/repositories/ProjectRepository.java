package com.tuannt.api.repositories;

import com.tuannt.api.entities.Project;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByVisibleTrue(Sort sort);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}

package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.constants.ValidationPatterns;
import com.tuannt.api.dtos.project.ProjectDto;
import com.tuannt.api.dtos.project.ProjectReqDto;
import com.tuannt.api.entities.Project;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.repositories.ProjectRepository;
import com.tuannt.api.services.DeployHookService;
import com.tuannt.api.services.ProjectService;
import com.tuannt.api.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private static final Sort DISPLAY_ORDER = Sort.by(
            Sort.Order.desc("featured"), Sort.Order.asc("sortOrder"), Sort.Order.asc("id"));

    private final ProjectRepository projectRepository;
    private final DeployHookService deployHookService;
    private final CommonUtil commonUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> listVisible() {
        return projectRepository.findByVisibleTrue(DISPLAY_ORDER).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> listAll() {
        return projectRepository.findAll(DISPLAY_ORDER).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto get(Long id) {
        return toDto(findProject(id));
    }

    @Override
    @Transactional
    public ProjectDto create(ProjectReqDto req) {
        String slug = StringUtils.hasText(req.getSlug()) ? req.getSlug() : deriveSlug(req.getName());
        checkSlug(slug, projectRepository.existsBySlug(slug));

        Instant now = Instant.now();
        Project project = new Project();
        project.setSlug(slug);
        project.setCreatedAt(now);
        apply(project, req, now);
        Project saved = projectRepository.save(project);
        log.info("Project created id: {} slug: {} visible: {}", saved.getId(), saved.getSlug(), saved.isVisible());
        if (saved.isVisible()) {
            deployHookService.contentChanged("project created: " + saved.getSlug());
        }
        return toDto(saved);
    }

    @Override
    @Transactional
    public ProjectDto update(Long id, ProjectReqDto req) {
        Project project = findProject(id);
        boolean wasVisible = project.isVisible();
        // An empty slug keeps the current one, so links to it keep working.
        if (StringUtils.hasText(req.getSlug()) && !req.getSlug().equals(project.getSlug())) {
            checkSlug(req.getSlug(), projectRepository.existsBySlugAndIdNot(req.getSlug(), id));
            project.setSlug(req.getSlug());
        }
        apply(project, req, Instant.now());
        log.info("Project updated id: {} slug: {} visible: {}", id, project.getSlug(), project.isVisible());
        if (wasVisible || project.isVisible()) {
            deployHookService.contentChanged("project updated: " + project.getSlug());
        }
        return toDto(project);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = findProject(id);
        projectRepository.delete(project);
        log.info("Project deleted id: {} slug: {}", id, project.getSlug());
        if (project.isVisible()) {
            deployHookService.contentChanged("project deleted: " + project.getSlug());
        }
    }

    private void apply(Project project, ProjectReqDto req, Instant now) {
        project.setName(req.getName().trim());
        project.setSummary(req.getSummary().trim());
        project.setDescription(StringUtils.hasText(req.getDescription()) ? req.getDescription() : null);
        project.setTech(commonUtil.toJsonArray(normalizeTech(req.getTech())));
        project.setRepoUrl(blankToNull(req.getRepoUrl()));
        project.setDemoUrl(blankToNull(req.getDemoUrl()));
        project.setImageUrl(blankToNull(req.getImageUrl()));
        project.setFeatured(req.isFeatured());
        project.setSortOrder(req.getSortOrder());
        project.setVisible(req.isVisible());
        project.setUpdatedAt(now);
    }

    /** Trimmed, case-insensitive duplicates dropped, original spelling and order kept. */
    private List<String> normalizeTech(List<String> tech) {
        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String item : tech) {
            String trimmed = item.trim();
            if (seen.add(trimmed.toLowerCase(Locale.ROOT))) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String deriveSlug(String name) {
        String slug = commonUtil.slugify(name, ValidationPatterns.SLUG_MAX_LENGTH);
        if (slug.isEmpty()) {
            throw new BadRequestException("slug could not be derived from the name, please provide one");
        }
        return slug;
    }

    private void checkSlug(String slug, boolean taken) {
        if (taken) {
            throw new BadRequestException(ApiStatus.SLUG_TAKEN, "Slug already exists: " + slug);
        }
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new NotFoundException(ApiStatus.PROJECT_NOT_FOUND));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ProjectDto toDto(Project p) {
        return ProjectDto.builder()
                .id(p.getId())
                .slug(p.getSlug())
                .name(p.getName())
                .summary(p.getSummary())
                .description(p.getDescription())
                .tech(commonUtil.fromJsonArray(p.getTech()))
                .repoUrl(p.getRepoUrl())
                .demoUrl(p.getDemoUrl())
                .imageUrl(p.getImageUrl())
                .featured(p.isFeatured())
                .sortOrder(p.getSortOrder())
                .visible(p.isVisible())
                .createdAt(p.getCreatedAt().toEpochMilli())
                .updatedAt(p.getUpdatedAt().toEpochMilli())
                .build();
    }
}

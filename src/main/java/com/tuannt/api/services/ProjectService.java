package com.tuannt.api.services;

import com.tuannt.api.dtos.project.ProjectDto;
import com.tuannt.api.dtos.project.ProjectReqDto;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 */
public interface ProjectService {
    /** Visible projects, featured first, then by sortOrder. */
    List<ProjectDto> listVisible();

    /** Same order, hidden ones included. */
    List<ProjectDto> listAll();

    ProjectDto get(Long id);

    ProjectDto create(ProjectReqDto req);

    ProjectDto update(Long id, ProjectReqDto req);

    void delete(Long id);
}

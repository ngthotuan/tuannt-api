package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.project.ProjectDto;
import com.tuannt.api.services.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * Public portfolio. A plain list rather than pages: a portfolio has a handful of entries.
 */

@Slf4j
@RestController
@RequestMapping(ApiPaths.API_PROJECT_PATH)
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    public List<ProjectDto> list() {
        return projectService.listVisible();
    }
}

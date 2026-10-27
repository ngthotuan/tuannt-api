package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.project.ProjectDto;
import com.tuannt.api.dtos.project.ProjectReqDto;
import com.tuannt.api.services.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * ADMIN only (enforced in SecurityConfig).
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_ADMIN_PATH + "/projects")
@RequiredArgsConstructor
public class AdminProjectController {
    private final ProjectService projectService;

    @GetMapping
    public List<ProjectDto> list() {
        return projectService.listAll();
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@Valid @RequestBody ProjectReqDto req) {
        return projectService.create(req);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectReqDto req) {
        return projectService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }
}

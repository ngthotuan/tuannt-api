package com.tuannt.api.services;

import com.tuannt.api.dtos.project.GithubRepoDto;

import java.util.List;

/**
 * Created by tuannt7 on 09/11/2026
 */
public interface GithubService {
    /** The configured user's public repositories, most recently pushed first. */
    List<GithubRepoDto> listRepos();
}

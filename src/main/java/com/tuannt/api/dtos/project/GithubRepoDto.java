package com.tuannt.api.dtos.project;

import java.util.List;

/**
 * A public GitHub repository offered for import as a project. {@code imported} is true when a
 * project already links to it.
 */
public record GithubRepoDto(
        String name,
        String description,
        String repoUrl,
        String homepage,
        String language,
        List<String> topics,
        int stars,
        boolean fork,
        boolean archived,
        Long pushedAt,
        boolean imported) {
}

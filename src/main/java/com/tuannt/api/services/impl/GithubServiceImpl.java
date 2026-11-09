package com.tuannt.api.services.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuannt.api.configs.GithubConfig;
import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.dtos.project.GithubRepoDto;
import com.tuannt.api.entities.Project;
import com.tuannt.api.exceptions.ExternalServiceException;
import com.tuannt.api.repositories.ProjectRepository;
import com.tuannt.api.services.GithubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by tuannt7 on 09/11/2026
 */
@Slf4j
@Service
public class GithubServiceImpl implements GithubService {
    private static final String API_BASE = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final GithubConfig githubConfig;
    private final ProjectRepository projectRepository;

    @Autowired
    public GithubServiceImpl(RestClient.Builder builder, GithubConfig githubConfig, ProjectRepository projectRepository) {
        this(client(builder.clone().requestFactory(timeoutRequestFactory()), githubConfig), githubConfig, projectRepository);
    }

    GithubServiceImpl(RestClient restClient, GithubConfig githubConfig, ProjectRepository projectRepository) {
        this.restClient = restClient;
        this.githubConfig = githubConfig;
        this.projectRepository = projectRepository;
    }

    /** Keeps the builder's request factory, so tests can bind a mock server to it. */
    static RestClient client(RestClient.Builder builder, GithubConfig githubConfig) {
        RestClient.Builder configured = builder.clone()
                .baseUrl(API_BASE)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                // GitHub rejects requests without a User-Agent.
                .defaultHeader("User-Agent", "tuannt-api");
        if (StringUtils.hasText(githubConfig.getToken())) {
            configured.defaultHeader("Authorization", "Bearer " + githubConfig.getToken());
        }
        return configured.build();
    }

    @Override
    public List<GithubRepoDto> listRepos() {
        List<GithubRepo> repos;
        try {
            GithubRepo[] body = restClient.get()
                    .uri("/users/{user}/repos?type=owner&sort=pushed&per_page=100", githubConfig.getUsername())
                    .retrieve()
                    .body(GithubRepo[].class);
            repos = body == null ? List.of() : List.of(body);
        } catch (RestClientException e) {
            log.warn("GitHub repo listing failed: {}", e.getMessage());
            throw new ExternalServiceException(ApiStatus.EXTERNAL_SERVER_ERROR);
        }

        Set<String> linked = projectRepository.findAll().stream()
                .map(Project::getRepoUrl)
                .filter(Objects::nonNull)
                .map(GithubServiceImpl::normalizeUrl)
                .collect(Collectors.toSet());

        return repos.stream()
                .map(repo -> new GithubRepoDto(
                        repo.name(),
                        repo.description(),
                        repo.htmlUrl(),
                        StringUtils.hasText(repo.homepage()) ? repo.homepage() : null,
                        repo.language(),
                        repo.topics() == null ? List.of() : repo.topics(),
                        repo.stars(),
                        repo.fork(),
                        repo.archived(),
                        repo.pushedAt() == null ? null : Instant.parse(repo.pushedAt()).toEpochMilli(),
                        linked.contains(normalizeUrl(repo.htmlUrl()))))
                .toList();
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return requestFactory;
    }

    /** github.com/User/Repo/ and github.com/user/repo point at the same repository. */
    static String normalizeUrl(String url) {
        String trimmed = url.trim().toLowerCase(Locale.ROOT);
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    /** The subset of GitHub's repository JSON that an import needs. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GithubRepo(
            String name,
            String description,
            @JsonProperty("html_url") String htmlUrl,
            String homepage,
            String language,
            List<String> topics,
            @JsonProperty("stargazers_count") int stars,
            boolean fork,
            boolean archived,
            @JsonProperty("pushed_at") String pushedAt) {
    }
}

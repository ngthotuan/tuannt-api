package com.tuannt.api.services.impl;

import com.tuannt.api.configs.GithubConfig;
import com.tuannt.api.dtos.project.GithubRepoDto;
import com.tuannt.api.entities.Project;
import com.tuannt.api.exceptions.ExternalServiceException;
import com.tuannt.api.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubServiceImplTest {
    private static final String REPOS_URL = "https://api.github.com/users/ngthotuan/repos?type=owner&sort=pushed&per_page=100";
    private static final String BODY = """
            [
              {"name": "tuannt-api", "description": "Site API", "html_url": "https://github.com/ngthotuan/tuannt-api",
               "homepage": "https://api.nttuan.dev", "language": "Java", "topics": ["spring-boot"],
               "stargazers_count": 3, "fork": false, "archived": false, "pushed_at": "2026-11-01T10:00:00Z", "extra": 1},
              {"name": "old-fork", "description": null, "html_url": "https://github.com/ngthotuan/old-fork",
               "homepage": "", "language": null, "stargazers_count": 0, "fork": true, "archived": true, "pushed_at": null}
            ]
            """;

    private MockRestServiceServer server;
    private GithubServiceImpl service;
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        GithubConfig config = new GithubConfig();
        config.setUsername("ngthotuan");
        service = new GithubServiceImpl(GithubServiceImpl.client(builder, config), config, projectRepository);
    }

    @Test
    void mapsReposAndFlagsTheOnesAlreadyImported() {
        Project existing = new Project();
        existing.setRepoUrl("https://github.com/NgThoTuan/tuannt-api/");
        when(projectRepository.findAll()).thenReturn(List.of(existing));
        server.expect(requestTo(REPOS_URL))
                .andExpect(header("User-Agent", "tuannt-api"))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        List<GithubRepoDto> repos = service.listRepos();

        assertThat(repos).hasSize(2);
        GithubRepoDto api = repos.getFirst();
        assertThat(api.name()).isEqualTo("tuannt-api");
        assertThat(api.language()).isEqualTo("Java");
        assertThat(api.topics()).containsExactly("spring-boot");
        assertThat(api.stars()).isEqualTo(3);
        assertThat(api.pushedAt()).isEqualTo(1_793_527_200_000L);
        assertThat(api.imported()).isTrue();

        GithubRepoDto fork = repos.get(1);
        assertThat(fork.homepage()).isNull();
        assertThat(fork.topics()).isEmpty();
        assertThat(fork.pushedAt()).isNull();
        assertThat(fork.fork()).isTrue();
        assertThat(fork.imported()).isFalse();
        server.verify();
    }

    @Test
    void upstreamFailureBecomesBadGateway() {
        server.expect(requestTo(REPOS_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.listRepos()).isInstanceOf(ExternalServiceException.class);
    }
}

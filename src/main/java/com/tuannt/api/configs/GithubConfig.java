package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by tuannt7 on 09/11/2026
 */
@Data
@ConfigurationProperties("github")
public class GithubConfig {
    /** Whose public repositories the admin can import as projects. */
    private String username;
    /** Optional: unauthenticated calls are limited to 60/hour per IP, which is plenty here. */
    private String token;
}

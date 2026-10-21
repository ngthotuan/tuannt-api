package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Created by tuannt7 on 21/10/2026
 */
@Data
@ConfigurationProperties("auth")
public class AuthConfig {
    /** HS256 key; must be at least 32 bytes. */
    private String jwtSecret;
    private Duration tokenTtl = Duration.ofDays(30);
    private Admin admin = new Admin();

    /**
     * Bootstrap account created at startup when it does not exist yet. Both values may be left
     * empty once the account is in the database.
     */
    @Data
    public static class Admin {
        private String username;
        private String password;
    }
}

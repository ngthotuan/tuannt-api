package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Created by tuannt7 on 31/08/2026
 */

@Data
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {
    private List<String> allowedOriginPatterns = List.of();
    private List<String> allowedMethods = List.of("GET", "POST", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private long maxAge = 3600;
}

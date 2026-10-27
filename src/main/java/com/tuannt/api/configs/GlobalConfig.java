package com.tuannt.api.configs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;


/**
 * Created by tuannt7 on 02/05/2023
 */

@Configuration
@RequiredArgsConstructor
public class GlobalConfig {
    private final CorsConfig corsConfig;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(corsConfig.getAllowedOriginPatterns().toArray(new String[0]))
                        .allowedMethods(corsConfig.getAllowedMethods().toArray(new String[0]))
                        .allowedHeaders(corsConfig.getAllowedHeaders().toArray(new String[0]))
                        .maxAge(corsConfig.getMaxAge());
            }
        };
    }


    /** Injected rather than calling Instant.now(), so review scheduling can run on a fixed clock in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new JavaTimeModule());
    }
}

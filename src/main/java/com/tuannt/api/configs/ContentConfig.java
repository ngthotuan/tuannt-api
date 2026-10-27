package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Created by tuannt7 on 27/10/2026
 */
@Data
@ConfigurationProperties("content")
public class ContentConfig {
    /** Cloudflare Pages deploy hook URL; empty disables the rebuild trigger. */
    private String deployHookUrl;
    /** Edits made within this window share one rebuild instead of queueing one each. */
    private Duration deployHookDelay = Duration.ofSeconds(10);
    private Duration deployHookTimeout = Duration.ofSeconds(10);
}

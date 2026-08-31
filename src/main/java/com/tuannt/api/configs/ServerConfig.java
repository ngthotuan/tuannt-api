package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by tuannt7 on 02/05/2023
 */
@Data
@ConfigurationProperties(prefix = "server.config")
public class ServerConfig {
    private int connectionTimeout;
    private long requestTimeout;
    private long readTimeout;
    private long writeTimeout;
    private long maxIdeTime;
    private int maxConnection;
    private long maxLifeTime;
}

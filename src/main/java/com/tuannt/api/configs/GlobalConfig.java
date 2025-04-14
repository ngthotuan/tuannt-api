package com.tuannt.api.configs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Created by tuannt7 on 02/05/2023
 */

@Configuration
@RequiredArgsConstructor
public class GlobalConfig {
    private final ServerConfig serverConfig;

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider =
                ConnectionProvider.builder("custom")
                        .maxConnections(serverConfig.getMaxConnection())
                        .maxIdleTime(Duration.ofMillis(serverConfig.getMaxIdeTime()))
                        .maxLifeTime(Duration.ofMillis(serverConfig.getMaxLifeTime())).build();
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, serverConfig.getConnectionTimeout())
                .responseTimeout(Duration.ofMillis(serverConfig.getRequestTimeout()))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(serverConfig.getReadTimeout(), TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(serverConfig.getWriteTimeout(), TimeUnit.MILLISECONDS)));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new JavaTimeModule())
                ;
    }
}

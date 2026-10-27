package com.tuannt.api.services.impl;

import com.tuannt.api.configs.ContentConfig;
import com.tuannt.api.services.DeployHookService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * POSTs the Cloudflare Pages deploy hook. Changes are coalesced: the first one schedules a
 * single call after {@code content.deployHookDelay}, and anything arriving before it fires
 * rides along, so saving a post five times in a row costs one build, not five.
 */
@Slf4j
@Service
public class DeployHookServiceImpl implements DeployHookService {
    private final ContentConfig config;
    private final HttpClient httpClient;
    private final AtomicBoolean pending = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "deploy-hook");
        thread.setDaemon(true);
        return thread;
    });

    @Autowired
    public DeployHookServiceImpl(ContentConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    DeployHookServiceImpl(ContentConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public void contentChanged(String reason) {
        if (!StringUtils.hasText(config.getDeployHookUrl())) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    schedule(reason);
                }
            });
        } else {
            schedule(reason);
        }
    }

    private void schedule(String reason) {
        if (!pending.compareAndSet(false, true)) {
            log.info("Deploy hook already scheduled, coalescing: {}", reason);
            return;
        }
        log.info("Deploy hook scheduled in {}: {}", config.getDeployHookDelay(), reason);
        try {
            scheduler.schedule(this::fire, config.getDeployHookDelay().toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException e) {
            pending.set(false);
            log.warn("Deploy hook could not be scheduled: {}", e.getMessage());
        }
    }

    private void fire() {
        // Cleared before sending, so a change made while the request is in flight schedules a
        // fresh build instead of being lost.
        pending.set(false);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.getDeployHookUrl()))
                    .timeout(config.getDeployHookTimeout())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            // The URL is the credential, so it is never logged; only the outcome is.
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            log.warn("Deploy hook failed: {}", error.toString());
                        } else if (response.statusCode() >= 300) {
                            log.warn("Deploy hook returned HTTP {}", response.statusCode());
                        } else {
                            log.info("Deploy hook accepted, HTTP {}", response.statusCode());
                        }
                    });
        } catch (RuntimeException e) {
            // Class name only: a malformed-URL message would echo the URL into the log.
            log.warn("Deploy hook failed: {}", e.getClass().getSimpleName());
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}

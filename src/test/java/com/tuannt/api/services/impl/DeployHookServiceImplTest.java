package com.tuannt.api.services.impl;

import com.tuannt.api.configs.ContentConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeployHookServiceImplTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final ContentConfig config = new ContentConfig();
    private DeployHookServiceImpl service;

    @AfterEach
    void stop() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    void doesNothingWhenNoHookIsConfigured() {
        config.setDeployHookUrl("");
        config.setDeployHookDelay(Duration.ZERO);
        service = new DeployHookServiceImpl(config, httpClient);

        service.contentChanged("post published: x");

        verify(httpClient, after(300).never()).sendAsync(any(), any());
        verifyNoInteractions(httpClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void postsToTheHookAndCoalescesBursts() {
        config.setDeployHookUrl("https://api.cloudflare.com/client/v4/pages/webhooks/deploy_hooks/test");
        config.setDeployHookDelay(Duration.ofMillis(200));
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(httpClient.sendAsync(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        service = new DeployHookServiceImpl(config, httpClient);

        service.contentChanged("one");
        service.contentChanged("two");
        service.contentChanged("three");

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        // Three changes inside the delay window make exactly one call.
        verify(httpClient, timeout(2000).times(1)).sendAsync(request.capture(), any());
        verify(httpClient, after(500).times(1)).sendAsync(any(), any());
        assertThat(request.getValue().method()).isEqualTo("POST");
        assertThat(request.getValue().uri()).isEqualTo(URI.create(config.getDeployHookUrl()));
    }

    @Test
    void aFailingHookNeverReachesTheCaller() {
        config.setDeployHookUrl("https://example.invalid/hook");
        config.setDeployHookDelay(Duration.ZERO);
        when(httpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));
        service = new DeployHookServiceImpl(config, httpClient);

        service.contentChanged("post deleted: x");

        verify(httpClient, timeout(2000)).sendAsync(any(), any());
    }
}

package com.tuannt.api.services.impl;

import com.tuannt.api.configs.NotifyConfig;
import com.tuannt.api.entities.ContactMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by tuannt7 on 10/11/2026
 * <p>
 * Emails the site owner when a contact message arrives. Sent after the message is committed, on
 * a virtual thread, and failures are only logged: the database row is the record, the email
 * just a nudge, and the visitor must never see an error because of it.
 */
@Slf4j
@Component
public class ContactNotifier {
    private static final String RESEND_URL = "https://api.resend.com/emails";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final NotifyConfig config;
    private final RestClient restClient;
    private final Executor executor;

    @Autowired
    public ContactNotifier(NotifyConfig config, RestClient.Builder builder) {
        this(config, builder.clone().requestFactory(timeouts()).build(), Executors.newVirtualThreadPerTaskExecutor());
    }

    ContactNotifier(NotifyConfig config, RestClient restClient, Executor executor) {
        this.config = config;
        this.restClient = restClient;
        this.executor = executor;
    }

    public void messageReceived(ContactMessage message) {
        if (!config.isEnabled()) {
            return;
        }
        Runnable send = () -> executor.execute(() -> send(message));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    void send(ContactMessage message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", config.getFrom());
        body.put("to", List.of(config.getTo()));
        // Replying in the mail client answers the visitor directly.
        body.put("reply_to", message.getEmail());
        body.put("subject", "[nttuan.dev] " + message.getSubject());
        body.put("text", """
                New message from the contact form

                From: %s <%s>
                Subject: %s

                %s

                --
                Message #%d · read and manage it at https://nttuan.dev/admin/inbox
                """.formatted(message.getName(), message.getEmail(), message.getSubject(), message.getMessage(), message.getId()));
        try {
            restClient.post()
                    .uri(RESEND_URL)
                    .header("Authorization", "Bearer " + config.getResendApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Contact alert emailed for message id: {}", message.getId());
        } catch (RuntimeException e) {
            log.warn("Contact alert email failed for message id: {}: {}", message.getId(), e.getMessage());
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }
}

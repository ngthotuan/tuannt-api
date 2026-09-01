package com.tuannt.api.services.impl;

import com.tuannt.api.dtos.contact.ContactMessageReqDto;
import com.tuannt.api.entities.ContactMessage;
import com.tuannt.api.exceptions.TooManyRequestsException;
import com.tuannt.api.repositories.ContactMessageRepository;
import com.tuannt.api.services.ContactService;
import com.tuannt.api.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {
    private static final int MAX_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ContactMessageRepository repository;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional
    public void submit(ContactMessageReqDto req, String ip, String userAgent) {
        if (!rateLimiter.tryAcquire(ip, MAX_PER_WINDOW, WINDOW)) {
            log.warn("Contact submit rate limited for ip: {}", ip);
            throw new TooManyRequestsException();
        }

        ContactMessage entity = new ContactMessage();
        entity.setName(req.getName().trim());
        entity.setEmail(req.getEmail().trim());
        entity.setSubject(req.getSubject().trim());
        entity.setMessage(req.getMessage().trim());
        entity.setIp(ip);
        entity.setUserAgent(truncate(userAgent, 500));
        entity.setCreatedAt(Instant.now());

        ContactMessage saved = repository.save(entity);
        log.info("Contact message saved id: {} from: {}", saved.getId(), saved.getEmail());

        notify(saved);
    }

    /**
     * Best-effort: the message is already persisted, so a failing notification must not turn a
     * successful submission into an error for the visitor. Telegram currently runs on placeholder
     * credentials, so this is expected to fail until real ones are configured.
     */
    private void notify(ContactMessage saved) {
        try {
            String body = String.format("From: %s <%s>%nSubject: %s%n%n%s",
                    saved.getName(), saved.getEmail(), saved.getSubject(), saved.getMessage());
            notificationService.sentMessage("New contact message", body);
        } catch (Exception e) {
            log.warn("Contact notification failed for id: {} - {}", saved.getId(), e.getMessage());
        }
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

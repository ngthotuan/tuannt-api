package com.tuannt.api.services.impl;

import com.tuannt.api.dtos.contact.ContactMessageReqDto;
import com.tuannt.api.entities.ContactMessage;
import com.tuannt.api.exceptions.TooManyRequestsException;
import com.tuannt.api.repositories.ContactMessageRepository;
import com.tuannt.api.services.ContactService;
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
    }


    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

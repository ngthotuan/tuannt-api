package com.tuannt.api.services.impl;

import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.contact.ContactMessageDto;
import com.tuannt.api.dtos.contact.ContactMessageReqDto;
import com.tuannt.api.entities.ContactMessage;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.exceptions.TooManyRequestsException;
import com.tuannt.api.repositories.ContactMessageRepository;
import com.tuannt.api.services.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Override
    @Transactional(readOnly = true)
    public PageResDto<ContactMessageDto> list(int page, int size, boolean unreadOnly) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<ContactMessage> result = unreadOnly
                ? repository.findByReadAtIsNull(pageable)
                : repository.findAll(pageable);
        return PageResDto.of(result, this::toDto);
    }

    @Override
    @Transactional
    public ContactMessageDto markRead(Long id) {
        ContactMessage message = findMessage(id);
        if (message.getReadAt() == null) {
            message.setReadAt(Instant.now());
        }
        return toDto(message);
    }

    @Override
    @Transactional
    public ContactMessageDto markUnread(Long id) {
        ContactMessage message = findMessage(id);
        message.setReadAt(null);
        return toDto(message);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ContactMessage message = findMessage(id);
        repository.delete(message);
        log.info("Contact message deleted id: {}", id);
    }

    private ContactMessage findMessage(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(ApiStatus.MESSAGE_NOT_FOUND));
    }

    private ContactMessageDto toDto(ContactMessage m) {
        return ContactMessageDto.builder()
                .id(m.getId())
                .name(m.getName())
                .email(m.getEmail())
                .subject(m.getSubject())
                .message(m.getMessage())
                .ip(m.getIp())
                .createdAt(m.getCreatedAt().toEpochMilli())
                .readAt(m.getReadAt() == null ? null : m.getReadAt().toEpochMilli())
                .build();
    }


    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

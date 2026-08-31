package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.contact.ContactMessageReqDto;
import com.tuannt.api.services.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by tuannt7 on 01/09/2026
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_CONTACT_PATH)
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;
    private final HttpServletRequest request;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@Valid @RequestBody ContactMessageReqDto req) {
        contactService.submit(req, clientIp(), request.getHeader("User-Agent"));
    }

    /**
     * Heroku terminates TLS at its router, so the real client address only survives in
     * X-Forwarded-For; the first entry is the original caller.
     */
    private String clientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

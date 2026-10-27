package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.contact.ContactMessageDto;
import com.tuannt.api.services.ContactService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * The contact form inbox. ADMIN only (enforced in SecurityConfig).
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_ADMIN_PATH + "/contact-messages")
@RequiredArgsConstructor
public class AdminContactController {
    private final ContactService contactService;

    @GetMapping
    public PageResDto<ContactMessageDto> list(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be at least 0")
            @Max(value = 100_000, message = "page must be at most 100000") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(defaultValue = "false") boolean unread) {
        return contactService.list(page, size, unread);
    }

    @PutMapping("/{id}/read")
    public ContactMessageDto markRead(@PathVariable Long id) {
        return contactService.markRead(id);
    }

    /** Undo for an accidental mark-as-read. */
    @DeleteMapping("/{id}/read")
    public ContactMessageDto markUnread(@PathVariable Long id) {
        return contactService.markUnread(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contactService.delete(id);
    }
}

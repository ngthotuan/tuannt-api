package com.tuannt.api.services;

import com.tuannt.api.dtos.PageResDto;
import com.tuannt.api.dtos.contact.ContactMessageDto;
import com.tuannt.api.dtos.contact.ContactMessageReqDto;

/**
 * Created by tuannt7 on 01/09/2026
 */
public interface ContactService {
    void submit(ContactMessageReqDto req, String ip, String userAgent);

    /** Newest first; unreadOnly narrows to messages never marked read. */
    PageResDto<ContactMessageDto> list(int page, int size, boolean unreadOnly);

    /** Idempotent: the first read time is kept. */
    ContactMessageDto markRead(Long id);

    ContactMessageDto markUnread(Long id);

    void delete(Long id);
}

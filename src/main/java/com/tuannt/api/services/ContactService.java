package com.tuannt.api.services;

import com.tuannt.api.dtos.contact.ContactMessageReqDto;

/**
 * Created by tuannt7 on 01/09/2026
 */
public interface ContactService {
    void submit(ContactMessageReqDto req, String ip, String userAgent);
}

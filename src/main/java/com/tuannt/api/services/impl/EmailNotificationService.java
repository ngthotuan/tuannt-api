package com.tuannt.api.services.impl;

import com.tuannt.api.services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created by tuannt7 on 02/05/2023
 */

@Slf4j
@Service("emailNotificationService")
public class EmailNotificationService implements NotificationService {
    @Override
    public boolean sentMessage(String to, String title, String message) {
        log.warn("EmailNotificationService not implement yet");
        return false;
    }

    @Override
    public boolean sentMessage(String title, String message) {
        log.warn("EmailNotificationService not implement yet");
        return false;
    }
}

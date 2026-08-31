package com.tuannt.api.services.impl;

import com.tuannt.api.configs.TelegramConfig;
import com.tuannt.api.dtos.TelegramResp;
import com.tuannt.api.services.NotificationService;
import com.tuannt.api.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by tuannt7 on 02/05/2023
 */
@Slf4j
@Primary
@Service("telegramNotificationService")
@RequiredArgsConstructor
public class TelegramNotificationService implements NotificationService {

    private static final int MAX_LENGTH = 4096;
    private final TelegramConfig telegramConfig;
    private final CommonUtil commonUtil;

    @Override
    public boolean sentMessage(String to, String title, String message) {
        try {
            if (!telegramConfig.isEnable()) {
                log.warn("Telegram sentMessage is disable");
                return false;
            }
            String text = title != null ? String.format("<b>%s</b>%n%s", title, message) : message;
            if (text.length() > MAX_LENGTH) {
                text = text.substring(0, MAX_LENGTH - 1);
            }
            Map<String, String> body = Map.of(
                    "chat_id", to,
                    "parse_mode", "html",
                    "text", text
            );
            String url = telegramConfig.getDomain() + "/bot" + telegramConfig.getToken() + "/sendMessage";
            TelegramResp telegramResp = commonUtil.sendPost(url, body, TelegramResp.class);
            return telegramResp != null && telegramResp.isSuccess();
        } catch (Exception e) {
            // Telegram tra 429 khi gui lien tuc qua nhieu; retry mot lan sau khoang cho no yeu cau.
            TelegramResp telegramResp = commonUtil.jsonStringToObject(e.getMessage(), TelegramResp.class);
            if (telegramResp != null && !telegramResp.isSuccess() && telegramResp.getErrorCode() == 429) {
                long retryMs = telegramResp.retryMs();
                log.warn("Telegram sentMessage rate limited, retry after: {} ms", retryMs);
                try {
                    Thread.sleep(retryMs);
                    return sentMessage(to, title, message);
                } catch (InterruptedException interruptedException) {
                    // Khoi phuc co interrupt, neu khong thread pool se mat tin hieu huy.
                    Thread.currentThread().interrupt();
                    log.error("Telegram sentMessage retry interrupted", interruptedException);
                    return false;
                }
            }
            log.error("Telegram sentMessage exception: ", e);
        }
        return false;
    }

    @Override
    public boolean sentMessage(String title, String message) {
        return sentMessage(telegramConfig.getChatId(), title, message);
    }
}

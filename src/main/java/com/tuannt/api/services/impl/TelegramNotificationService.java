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

    @Override
    public boolean sentMessage(String to, String title, String message) {
        try {
            if (!telegramConfig.isEnable()) {
                log.warn("Telegram sentMessage is disable");
                return false;
            }
            if (message.length() > MAX_LENGTH) {
                message = message.substring(0, MAX_LENGTH - 1);
            }
            Map<String, String> body = Map.of(
                    "chat_id", to,
                    "parse_mode", "html",
//                    "text", title != null ? String.format("<b>%s</b>\n%s", title, message) : message
                    "text", message
            );
            String url = telegramConfig.getDomain() + "/bot" + telegramConfig.getToken() + "/sendMessage";
            TelegramResp telegramResp = CommonUtil.sendPost(url, body, TelegramResp.class);
            return telegramResp != null && telegramResp.isSuccess();
        } catch (Exception e) {
            // đã có cơ chế sau 1 khoảng tg (100ms) lấy message gửi 1 lần
            // tuy nhiên nếu gửi notification lên telegram liên tục nhiều quá bị block lỗi 429
            TelegramResp telegramResp = CommonUtil.jsonStringToObject(e.getMessage(), TelegramResp.class);
            if (telegramResp != null) {
                log.error("Telegram sentMessage error blocked !!!");
                if (!telegramResp.isSuccess() && telegramResp.getErrorCode() == 429) {
                    long retryMs = telegramResp.retryMs();
                    log.warn("Telegram sentMessage error, sleep: {} ms", retryMs);
                    try {
                        Thread.sleep(retryMs);
                        sentMessage(title, message);
                    } catch (InterruptedException interruptedException) {
                        log.error("Telegram sentMessage retry exception: ", interruptedException);
                    }
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

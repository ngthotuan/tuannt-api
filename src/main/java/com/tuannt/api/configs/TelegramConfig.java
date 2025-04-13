package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by tuannt7 on 02/05/2023
 */
@Data
@ConfigurationProperties("notification.telegram")
public class TelegramConfig {
    private boolean enable;
    private String domain;
    private String token;
    private String chatId;
}

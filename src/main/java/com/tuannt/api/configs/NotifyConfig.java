package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Created by tuannt7 on 10/11/2026
 * <p>
 * Email alerts for new contact messages, sent through Resend. Disabled while the API key or the
 * recipient is empty, so the app runs the same without them.
 */
@Data
@ConfigurationProperties("notify")
public class NotifyConfig {
    private String resendApiKey;
    /** Where alerts go. */
    private String to;
    /** Must be on a domain verified in Resend. */
    private String from;

    public boolean isEnabled() {
        return StringUtils.hasText(resendApiKey) && StringUtils.hasText(to) && StringUtils.hasText(from);
    }
}

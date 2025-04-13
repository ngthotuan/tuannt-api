package com.tuannt.api.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by tuannt7 on 21/05/2024
 */

@Data
@ConfigurationProperties("adyen")
public class AdyenConfig {
    private String merchantAccount;
    private String apiKey;
    private String urlPayments;
    private String urlDetails;
    private String urlPmMethod;
    private String urlListDetails;
    private String redirectUrl;
}

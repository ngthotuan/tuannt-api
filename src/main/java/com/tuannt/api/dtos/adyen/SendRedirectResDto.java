package com.tuannt.api.dtos.adyen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by tuannt7 on 19/08/2024
 */
@Data
public class SendRedirectResDto {
    private AdditionalData additionalData;
    private String pspReference;
    private String resultCode;
    private String merchantReference;

    public String getShopperReference() {
        return additionalData.getShopperReference();
    }

    public String getRecurringDetailReference() {
        return additionalData.getRecurringDetailReference();
    }

    @Data
    private static class AdditionalData {
        @JsonProperty("recurring.shopperReference")
        private String shopperReference;
        @JsonProperty("recurring.recurringDetailReference")
        private String recurringDetailReference;
    }
}

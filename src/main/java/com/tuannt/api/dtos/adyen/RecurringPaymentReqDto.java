package com.tuannt.api.dtos.adyen;

import com.tuannt.api.dtos.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by tuannt7 on 21/05/2024
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringPaymentReqDto extends BaseDto {
    private String reference;
    private Amount amount;
    private String merchantAccount;
    private String returnUrl;
    private PaymentMethod paymentMethod;
    private String shopperReference;
    private boolean storePaymentMethod;
    private String recurringProcessingModel;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Amount extends BaseDto {
        private long value;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethod extends BaseDto {
        private String type;
        private String storedPaymentMethodId;
    }
}

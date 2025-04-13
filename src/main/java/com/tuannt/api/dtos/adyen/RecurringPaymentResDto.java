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
public class RecurringPaymentResDto extends BaseDto {
    private String resultCode;
    private Action action;
    private String refusalReasonCode;
    private String errorCode;
    private String pspReference;
    private String status;
    private String message;


    @Data
    public static class Action {
        private String url;
        private String method;
        private String paymentMethodType;
        private String type;
    }
}

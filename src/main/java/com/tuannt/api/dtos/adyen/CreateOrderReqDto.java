package com.tuannt.api.dtos.adyen;

import com.tuannt.api.dtos.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class CreateOrderReqDto extends BaseDto {
    private long amount;
    private String currency;
    private String method;
    private String userId;
    private String appId = "demo";
}

package com.tuannt.api.services;

import com.tuannt.api.dtos.adyen.CreateOrderReqDto;
import com.tuannt.api.dtos.adyen.RecurringPaymentResDto;

/**
 * Created by tuannt7 on 21/05/2024
 */

public interface AdyenService {
    RecurringPaymentResDto createOrder(CreateOrderReqDto createOrderReqDto);

    String redirectWeb(String redirectResult);

    String getDetails(String appId, String userId);

    String del(String appId, String userId, String token);
}

package com.tuannt.api.services.impl;

import com.tuannt.api.configs.AdyenConfig;
import com.tuannt.api.dtos.DataResDto;
import com.tuannt.api.dtos.adyen.CreateOrderReqDto;
import com.tuannt.api.dtos.adyen.RecurringPaymentReqDto;
import com.tuannt.api.dtos.adyen.RecurringPaymentResDto;
import com.tuannt.api.dtos.adyen.SendRedirectResDto;
import com.tuannt.api.services.AdyenService;
import com.tuannt.api.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tuannt7 on 21/05/2024
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AdyenServiceImpl implements AdyenService {
    private static final Map<String, String> USER_TOKEN = new HashMap<>();

    static {
        USER_TOKEN.put("demo@tuannt7", "SKNHNPKMXWXHVHV5");
    }

    private final AdyenConfig adyenConfig;

    @Override
    public RecurringPaymentResDto createOrder(CreateOrderReqDto req) {
        String shopperReference = req.getAppId() + "@" + req.getUserId();
        String transId = req.getAppId() + "_" + new SimpleDateFormat("yyMMddHHmmssSSSSSS")
                .format(System.currentTimeMillis());

        RecurringPaymentReqDto rpReq;
        if (!USER_TOKEN.containsKey(shopperReference)) {
            rpReq = RecurringPaymentReqDto.builder()
                    .reference(transId)
                    .amount(new RecurringPaymentReqDto.Amount(req.getAmount(), req.getCurrency()))
                    .merchantAccount(adyenConfig.getMerchantAccount())
                    .returnUrl(adyenConfig.getRedirectUrl())
                    .paymentMethod(new RecurringPaymentReqDto.PaymentMethod(req.getMethod(), null))
                    .shopperReference(shopperReference)
                    .storePaymentMethod(true)
                    .recurringProcessingModel("UnscheduledCardOnFile")
                    .build();
        } else {
            rpReq = RecurringPaymentReqDto.builder()
                    .reference(transId)
                    .amount(new RecurringPaymentReqDto.Amount(req.getAmount(), req.getCurrency()))
                    .merchantAccount(adyenConfig.getMerchantAccount())
                    .returnUrl(adyenConfig.getRedirectUrl())
                    .paymentMethod(new RecurringPaymentReqDto.PaymentMethod(req.getMethod(), USER_TOKEN.get(shopperReference)))
                    .shopperReference(shopperReference)
//                    .storePaymentMethod(true)
                    .recurringProcessingModel("UnscheduledCardOnFile")
                    .build();
        }
        log.info("Recurring payment request: {}", rpReq);

        DataResDto<RecurringPaymentResDto> res = CommonUtil.sendPostV2(adyenConfig.getUrlPayments(), getHeaders(), rpReq, RecurringPaymentResDto.class);
        log.info("Create order response: {}", res);
        return res.getData();
    }

    @Override
    public String redirectWeb(String redirectResult) {
        Map<String, Map<String, String>> req = new HashMap<>();
        req.put("details", Map.of("redirectResult", redirectResult));

        DataResDto<String> res = CommonUtil.sendPostV2(adyenConfig.getUrlDetails(), getHeaders(), req, String.class);
        log.info("Redirect web response: {}", res);
        SendRedirectResDto redirectResDto = CommonUtil.jsonStringToObject(res.getData(), SendRedirectResDto.class);
        if (redirectResDto != null && "Authorised".equalsIgnoreCase(redirectResDto.getResultCode())) {
            String shopperReference = redirectResDto.getShopperReference();
            String recurringDetailReference = redirectResDto.getRecurringDetailReference();
            log.info("Redirect web success save user token {} => {}", shopperReference, recurringDetailReference);
            USER_TOKEN.put(shopperReference, recurringDetailReference);
        }
        return res.getData();
    }

    @Override
    public String getDetails(String appId, String userId) {
        String shopperReference = appId + "@" + userId;
        log.info("Get details request: {}", shopperReference);
        Map<String, String> body = new HashMap<>();
        body.put("shopperReference", shopperReference);
        body.put("merchantAccount", adyenConfig.getMerchantAccount());
        DataResDto<String> res = CommonUtil.sendPostV2(adyenConfig.getUrlListDetails(), getHeaders(), body, String.class);
        return res.getData();
    }

    @Override
    public String del(String appId, String userId, String token) {
        String shopperReference = appId + "@" + userId;
        log.info("DEL request: {}", shopperReference);
        String query = "?shopperReference=" + shopperReference + "&merchantAccount=" + adyenConfig.getMerchantAccount();
        String url = adyenConfig.getUrlPmMethod() + "/" + token + query;
        DataResDto<String> res = CommonUtil.sendRequestV2(url, HttpMethod.DELETE, MediaType.APPLICATION_JSON, getHeaders(), "", String.class);
        return res.getData();
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-API-Key", adyenConfig.getApiKey());
        return headers;
    }
}

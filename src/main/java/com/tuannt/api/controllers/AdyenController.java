package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.adyen.CreateOrderReqDto;
import com.tuannt.api.dtos.adyen.RecurringPaymentResDto;
import com.tuannt.api.services.AdyenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Created by tuannt7 on 21/05/2024
 */

@Slf4j
@RestController
@RequestMapping(ApiPaths.API_ADYEN_PATH)
@RequiredArgsConstructor
public class AdyenController {
    private final AdyenService adyenService;
    private final HttpServletResponse response;

    // http://localhost:8089/api/v1/adyen/createOrder?amount=10&currency=IDR&method=dana&userId=tuannt7&appId=pwm_id
    @RequestMapping("/createOrder")
    public void createOrder(CreateOrderReqDto req) throws IOException {
        log.info("createOrder request: {}", req);
        RecurringPaymentResDto res = adyenService.createOrder(req);
        if ("RedirectShopper".equalsIgnoreCase(res.getResultCode())) {
            String redirectUrl = res.getAction().getUrl();
            log.info("createOrder redirectURL: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            response.getWriter().write(res.toString());
        }
    }

    @RequestMapping("/rd")
    public String redirectWeb(@RequestParam String redirectResult) {
        log.info("redirectWeb request: {}", redirectResult);
        String res = adyenService.redirectWeb(redirectResult);
        log.info("redirectWeb response: {}", res);
        return res;
    }

    // http://localhost:8089/api/v1/adyen/details?appId=demo&userId=tuannt7
    @GetMapping("/details")
    public String getDetails(@RequestParam String appId, @RequestParam String userId) {
        log.info("getDetails request: appId: {} userId: {}", appId, userId);
        return adyenService.getDetails(appId, userId);
    }

    // http://localhost:8089/api/v1/adyen/del?appId=demo&userId=tuannt7&token=ZKBK9583KQLV7L65
    @GetMapping("/del")
    public String deleteToken(@RequestParam String appId, @RequestParam String userId, @RequestParam String token) {
        log.info("deleteToken request: appId: {} userId: {} token: {}", appId, userId, token);
        return adyenService.del(appId, userId, token);
    }
}

package com.tuannt.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by tuannt7 on 08/03/2023
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonUtil {
    private final ObjectMapper objectMapper;

    /**
     * The caller's address behind Heroku's router. The router appends the address it actually
     * saw to the END of X-Forwarded-For; anything before it came from the client and can be
     * forged, so the first entry must never be trusted (it once let anyone dodge every per-IP
     * rate limit by sending a fresh fake header with each request).
     */
    public String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    public String objectToJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("write object as string fail {}", e.getMessage(), e);
            return null;
        }
    }
}

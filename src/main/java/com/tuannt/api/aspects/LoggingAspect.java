package com.tuannt.api.aspects;

import com.tuannt.api.exceptions.BaseException;
import com.tuannt.api.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tuannt7 on 29/08/2023
 */
@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAspect {
    private final HttpServletRequest request;

    @SneakyThrows
    @Around("execution(public * com.tuannt.api.controllers.*.*(..)) && args(.., body)")
    public Object logControllers(final ProceedingJoinPoint joinPoint, final Object body) {
        String url = request.getRequestURL().toString();
        String httpMethod = request.getMethod();
        String qs = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        long startTime = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            if (e instanceof BaseException || e instanceof ConstraintViolationException) {
                log.info("{} {}{} from {}:{} - headers: {} - body: {} - execTime: {}ms - error: {}",
                        httpMethod, url, qs, request.getRemoteAddr(), request.getRemotePort(), getHeaders(),
                        body, (System.currentTimeMillis() - startTime), e.getMessage());
            } else {
                log.error("{} {}{} from {}:{} - headers: {} - body: {} - execTime: {}ms - error: {}",
                        httpMethod, url, qs, request.getRemoteAddr(), request.getRemotePort(), getHeaders(),
                        body, (System.currentTimeMillis() - startTime), e.getMessage(), e);
            }
            throw e;
        }
        log.info("{} {}{} from {}:{} - headers: {} - body: {} - execTime: {}ms",
                httpMethod, url, qs, request.getRemoteAddr(), request.getRemotePort(), getHeaders(),
                body, System.currentTimeMillis() - startTime);
        return result;
    }

    private String getHeaders() {
        Map<String, Object> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        return CommonUtil.objectToJsonString(headers);
    }
}


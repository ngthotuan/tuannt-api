package com.tuannt.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuannt.api.dtos.DataResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by tuannt7 on 08/03/2023
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonUtil {
    private static final TypeReference<Map<String, String>> MAP_REF = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public String objectToJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("write object as string fail {}", e.getMessage(), e);
            return null;
        }
    }

    public <T> T jsonStringToObject(String json, Class<T> className) {
        try {
            return objectMapper.readValue(json, className);
        } catch (Exception e) {
            log.error("parse json: {}, exception: ", json, e);
            return null;
        }
    }

    private <T> T convertObject(Object src, TypeReference<T> valueTypeRef) {
        try {
            return objectMapper.convertValue(src, valueTypeRef);
        } catch (Exception e) {
            log.error("convertObject src: {} => ref: {}, exception: ", src, valueTypeRef, e);
            return null;
        }
    }

    public <T, V> V sendPost(String url, T body, Class<V> returnType) {
        long startTime = System.currentTimeMillis();
        V resp = null;
        String exception = null;
        AtomicReference<HttpStatusCode> code = new AtomicReference<>();
        try {
            resp = webClient
                    .method(HttpMethod.POST)
                    .uri(url)
                    .bodyValue(body)
                    .exchangeToMono(clientResponse -> {
                        code.set(clientResponse.statusCode());
                        if (clientResponse.statusCode().is2xxSuccessful()) {
                            return clientResponse.bodyToMono(returnType);
                        } else {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(s -> Mono.error(new RuntimeException(s)));
                        }
                    })
                    .block();
        } catch (Exception ex) {
            exception = ex.getMessage() != null ? ex.getMessage().replace("\r\n", "") : ex.toString();
            log.error("sendPost exception: {} - {}", code, exception);
            throw ex;
        } finally {
            log.info("POST {} - body: {} - code: {} - resp: {} - execTime: {}",
                    url, body, code, resp != null ? objectToJsonString(resp) : exception,
                    System.currentTimeMillis() - startTime);
        }
        return resp;
    }

    public <T, V> DataResDto<V> sendRequestV2(String url, HttpMethod method, MediaType contentType, Map<String, String> headers, T body, Class<V> returnType) {
        long startTime = System.currentTimeMillis();
        V resp = null;
        String exception = null;
        AtomicReference<HttpStatusCode> code = new AtomicReference<>();
        try {
            WebClient.RequestBodySpec bodySpec = webClient
                    .method(method)
                    .uri(url);
            WebClient.RequestHeadersSpec<?> headersSpec;
            if (headers != null) {
                headers.forEach(bodySpec::header);
            }
            if (contentType == MediaType.APPLICATION_FORM_URLENCODED) {
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                Map<String, String> fieldMap = convertObject(body, MAP_REF);
                formData.setAll(fieldMap);
                headersSpec = bodySpec.contentType(contentType).body(BodyInserters.fromFormData(formData));
            } else {
                headersSpec = bodySpec.bodyValue(body);
            }
            resp = headersSpec.exchangeToMono(clientResponse -> {
                        code.set(clientResponse.statusCode());
                        if (clientResponse.statusCode().is2xxSuccessful() || clientResponse.statusCode().is4xxClientError()) {
                            return clientResponse.bodyToMono(returnType);
                        } else {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(s -> Mono.error(new RuntimeException(s)));
                        }
                    })
                    .block();
        } catch (Exception ex) {
            exception = ex.getMessage() != null ? ex.getMessage().replace("\r\n", "") : ex.toString();
            log.error("sendRequestV2 exception: {} - {}", code, exception, ex);
            throw ex;
        } finally {
            log.info("{} {} - headers: {} - body: {} - code: {} - resp: {} - execTime: {}",
                    method, url, headers, body, code, resp != null ? objectToJsonString(resp) : exception,
                    System.currentTimeMillis() - startTime);
        }
        return new DataResDto<>(code.get(), resp);
    }

    public <T, V> DataResDto<V> sendPostV2(String url, Map<String, String> headers, T body, Class<V> returnType) {
        return sendRequestV2(url, HttpMethod.POST, null, headers, body, returnType);
    }
}

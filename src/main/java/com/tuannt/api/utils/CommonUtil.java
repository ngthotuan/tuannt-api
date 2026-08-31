package com.tuannt.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuannt.api.dtos.DataResDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by tuannt7 on 08/03/2023
 */
@Slf4j
@Component
public class CommonUtil {
    public static final TypeReference<Map<String, String>> MAP_REF = new TypeReference<>() {
    };
    private static final Logger requestError = LoggerFactory.getLogger("RequestError");
    private static ObjectMapper objectMapper;
    private static WebClient webClient;

    @Autowired
    public CommonUtil(ObjectMapper objectMapper, WebClient webClient) {
        CommonUtil.objectMapper = objectMapper;
        CommonUtil.webClient = webClient;
    }

    public static boolean isPositiveNumber(Number number) {
        return number != null && number.doubleValue() > 0;
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            StringBuilder hashText = new StringBuilder(number.toString(16));

            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            return hashText.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("md5 data error: {}", e.getMessage(), e);
            return null;
        }
    }

    private static String sha(String algorithm, String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("sha data for sha {} error: {}", algorithm, e.getMessage(), e);
            return null;
        }
    }

    public static String sha256(String input) {
        return sha("SHA-256", input);
    }


    public static String objectToJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("write object as string fail {}", e.getMessage(), e);
            return null;
        }
    }

    public static <T> T jsonStringToObject(String json, Class<T> className) {
        try {
            return objectMapper.readValue(json, className);
        } catch (Exception e) {
            log.error("parse json: {}, exception: ", json, e);
            return null;
        }
    }


    public static <T> T convertObject(Object src, Class<T> destClass) {
        try {
            return objectMapper.convertValue(src, destClass);
        } catch (Exception e) {
            log.error("convertObject src: {} => destClass: {}, exception: ", src, destClass, e);
            return null;
        }
    }

    public static <T> T convertObject(Object src, TypeReference<T> valueTypeRef) {
        try {
            return objectMapper.convertValue(src, valueTypeRef);
        } catch (Exception e) {
            log.error("convertObject src: {} => ref: {}, exception: ", src, valueTypeRef, e);
            return null;
        }
    }

    public static <T> T jsonStringToObject(String json, TypeReference<T> valueTypeRef) {
        try {
            return objectMapper.readValue(json, valueTypeRef);
        } catch (Exception e) {
            log.error("parse json: {}, exception: ", json, e);
            return null;
        }
    }

    public static <T, V> V sendRequest(String url, HttpMethod method, T body, Class<V> returnType) {
        long startTime = System.currentTimeMillis();
        V resp = null;
        String exception = null;
        AtomicReference<HttpStatusCode> code = new AtomicReference<>();
        try {
            resp = webClient
                    .method(method)
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
            log.error("sendRequest exception: {} - {}", code, exception);
            throw ex;
        } finally {
            long endTime = System.currentTimeMillis();
            long execTime = endTime - startTime;
            log.info("{} {} - body: {} - code: {} - resp: {} - execTime: {}",
                    method, url, body, code, resp != null ? CommonUtil.objectToJsonString(resp) : exception, execTime);
        }
        return resp;
    }

    public static <T, V> V sendPost(String url, T body, Class<V> returnType) {
        return sendRequest(url, HttpMethod.POST, body, returnType);
    }

    public static <T, V> V sendGet(String url, T body, Class<V> returnType) {
        return sendRequest(url, HttpMethod.GET, body, returnType);
    }

    public static <T, V> DataResDto<V> sendRequestV2(String url, HttpMethod method, MediaType contentType, Map<String, String> headers, T body, Class<V> returnType) {
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
            requestError.error("{} {} - headers: {} - body: {} - code: {} - exception: {} - execTime: {}",
                    method, url, headers, body, code, exception, System.currentTimeMillis() - startTime, ex);
            throw ex;
        } finally {
            long endTime = System.currentTimeMillis();
            long execTime = endTime - startTime;
            log.info("{} {} - headers: {} - body: {} - code: {} - resp: {} - execTime: {}",
                    method, url, headers, body, code, resp != null ? CommonUtil.objectToJsonString(resp) : exception, execTime);
        }
        return new DataResDto<>(code.get(), resp);
    }

    public static <T, V> DataResDto<V> sendRequestV2(String url, HttpMethod method, T body, Class<V> returnType) {
        return sendRequestV2(url, method, null, null, body, returnType);
    }

    public static <T, V> DataResDto<V> sendPostV2(String url, Map<String, String> headers, T body, Class<V> returnType) {
        return sendRequestV2(url, HttpMethod.POST, null, headers, body, returnType);
    }

    public static <T, V> DataResDto<V> sendPostFormV2(String url, T body, Class<V> returnType) {
        return sendRequestV2(url, HttpMethod.POST, MediaType.APPLICATION_FORM_URLENCODED, null, body, returnType);
    }

    public static <T, V> DataResDto<V> sendPostFormV2(String url, Map<String, String> headers, T body, Class<V> returnType) {
        return sendRequestV2(url, HttpMethod.POST, MediaType.APPLICATION_FORM_URLENCODED, headers, body, returnType);
    }

    public static <T, V> DataResDto<V> sendGetV2(String url, Map<String, String> headers, T body, Class<V> returnType) {
        return sendRequestV2(url, HttpMethod.GET, null, headers, body, returnType);
    }

    public static <T, V> DataResDto<V> sendPostV2(String url, T body, Class<V> returnType) {
        return sendRequestV2(url, HttpMethod.POST, body, returnType);
    }

    public static <T, V> DataResDto<V> sendGetV2(String url, T body, Class<V> returnType) {
        return sendRequestV2(url, HttpMethod.GET, body, returnType);
    }

    public static String mapToQueryString(Map<String, String> body) {
        StringBuilder query = new StringBuilder();
        body.forEach((key, value) -> {
            if (value != null) {
                query.append(key).append("=").append(value).append("&");
            }
        });
        return query.toString();
    }
}

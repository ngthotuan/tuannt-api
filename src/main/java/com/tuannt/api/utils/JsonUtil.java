package com.tuannt.api.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by tuannt7 on 31/08/2026
 * <p>
 * Serialize dung cho muc dich log/toString. Co ObjectMapper static final rieng thay vi
 * muon bean cua Spring, vi {@link com.tuannt.api.dtos.BaseDto} la POJO thuong khong the
 * inject dependency. Day la hang so that su, khong phai state bi gan tu ben ngoai.
 */
@Slf4j
public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new JavaTimeModule());

    private JsonUtil() {
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            log.error("write object as string fail {}", e.getMessage(), e);
            return String.valueOf(object.getClass().getSimpleName());
        }
    }
}

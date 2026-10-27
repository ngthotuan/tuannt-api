package com.tuannt.api.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by tuannt7 on 31/08/2026
 * <p>
 * Serialization for logging / toString. Holds its own static final ObjectMapper rather than
 * borrowing the Spring bean, because {@link com.tuannt.api.dtos.BaseDto} is a plain POJO that
 * cannot be injected. This is a genuine constant, not state assigned from outside.
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

    /**
     * Like {@link #toJson(Object)} but replaces the given text fields with their length, for DTOs
     * that carry whole Markdown documents a log line has no use for.
     */
    public static String toJsonAbbreviated(Object object, String... fields) {
        try {
            JsonNode tree = MAPPER.valueToTree(object);
            if (tree instanceof ObjectNode node) {
                for (String field : fields) {
                    JsonNode value = node.get(field);
                    if (value != null && value.isTextual()) {
                        node.put(field, "<" + value.asText().length() + " chars>");
                    }
                }
            }
            return MAPPER.writeValueAsString(tree);
        } catch (Exception e) {
            log.error("write object as string fail {}", e.getMessage(), e);
            return String.valueOf(object.getClass().getSimpleName());
        }
    }
}

package com.tuannt.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by tuannt7 on 08/03/2023
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonUtil {
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * ASCII kebab-case for URLs: "Xin chào Đà Nẵng!" becomes "xin-chao-da-nang". NFD splits the
     * Vietnamese tone and vowel marks off their letters so they can be dropped; đ/Đ is a
     * separate letter rather than d plus a mark, so it needs its own replacement. The result is
     * cut to maxLength without leaving a trailing dash, and may be empty.
     */
    public String slugify(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String ascii = COMBINING_MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D');
        String slug = NON_SLUG_CHARS.matcher(ascii.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = trimDashes(slug);
        if (slug.length() > maxLength) {
            slug = trimDashes(slug.substring(0, maxLength));
        }
        return slug;
    }

    /** Lists of short strings are stored as JSON text so the same column works on MySQL and TiDB. */
    public String toJsonArray(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize string list", e);
        }
    }

    public List<String> fromJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            log.error("stored string list is not valid JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String trimDashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') start++;
        while (end > start && value.charAt(end - 1) == '-') end--;
        return value.substring(start, end);
    }

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

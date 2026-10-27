package com.tuannt.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonUtilTest {
    private final CommonUtil util = new CommonUtil(new ObjectMapper());

    @Test
    void clientIpIsTheHopHerokuAppended() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.1.1.1, 10.0.0.2 ,  203.0.113.9 ");
        assertThat(util.clientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void clientIpFallsBackToTheSocketAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.4");
        assertThat(util.clientIp(request)).isEqualTo("198.51.100.4");
    }

    @Test
    void slugifyStripsVietnameseDiacritics() {
        assertThat(util.slugify("Hướng dẫn Spring Boot 3 cho người mới: Đà Nẵng!", 120))
                .isEqualTo("huong-dan-spring-boot-3-cho-nguoi-moi-da-nang");
        assertThat(util.slugify("ĐỘC LẬP – Tự do – Hạnh phúc", 120)).isEqualTo("doc-lap-tu-do-hanh-phuc");
    }

    @Test
    void slugifyCutsWithoutATrailingDash() {
        assertThat(util.slugify("abc def", 4)).isEqualTo("abc");
        assertThat(util.slugify("  --Hello__World--  ", 120)).isEqualTo("hello-world");
        assertThat(util.slugify("!!!", 120)).isEmpty();
        assertThat(util.slugify(null, 120)).isEmpty();
    }

    @Test
    void stringListsRoundTripAsJson() {
        String json = util.toJsonArray(List.of("C#", "Node.js, Express"));
        assertThat(util.fromJsonArray(json)).containsExactly("C#", "Node.js, Express");
        assertThat(util.fromJsonArray(null)).isEmpty();
        assertThat(util.fromJsonArray("not json")).isEmpty();
    }
}

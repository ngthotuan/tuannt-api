package com.tuannt.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
}

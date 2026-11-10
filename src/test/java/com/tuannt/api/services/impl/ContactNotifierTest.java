package com.tuannt.api.services.impl;

import com.tuannt.api.configs.NotifyConfig;
import com.tuannt.api.entities.ContactMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ContactNotifierTest {

    private static NotifyConfig config(String key) {
        NotifyConfig config = new NotifyConfig();
        config.setResendApiKey(key);
        config.setTo("owner@example.com");
        config.setFrom("nttuan.dev <contact@nttuan.dev>");
        return config;
    }

    private static ContactMessage message() {
        ContactMessage message = new ContactMessage();
        message.setId(7L);
        message.setName("Lan");
        message.setEmail("lan@example.com");
        message.setSubject("Hello");
        message.setMessage("Xin chào!");
        return message;
    }

    @Test
    void sendsAnAlertThatRepliesToTheVisitor() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test"))
                .andExpect(jsonPath("$.to[0]").value("owner@example.com"))
                .andExpect(jsonPath("$.reply_to").value("lan@example.com"))
                .andExpect(jsonPath("$.subject").value("[nttuan.dev] Hello"))
                .andRespond(withSuccess("{\"id\":\"x\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        new ContactNotifier(config("re_test"), builder.build(), Runnable::run).messageReceived(message());
        server.verify();
    }

    @Test
    void failuresAreSwallowed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        new ContactNotifier(config("re_test"), builder.build(), Runnable::run).messageReceived(message());
        server.verify();
    }

    @Test
    void doesNothingWithoutAKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(never(), requestTo("https://api.resend.com/emails"));

        new ContactNotifier(config(""), builder.build(), Runnable::run).messageReceived(message());
        server.verify();
    }
}

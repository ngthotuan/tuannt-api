package com.tuannt.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by tuannt7 on 02/05/2023
 */
@Data
public class TelegramResp {
    private String ok;
    @JsonProperty("error_code")
    private Long errorCode;
    private String description;
    private Parameters parameters;

    public boolean isSuccess() {
        return "true".equalsIgnoreCase(ok);
    }

    public long retryMs() {
        return parameters.getRetryAfter() * 1000;
    }

    @Data
    private static class Parameters {
        @JsonProperty("retry_after")
        private long retryAfter;
    }

}

package com.tuannt.api.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuannt.api.constants.ApiStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by tuannt7 on 24/08/2023
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError implements Serializable {
    private String errorCode;
    private String errorMessage;

    public ApiError(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ApiError(ApiStatus apiStatus) {
        this.errorCode = apiStatus.name();
        this.errorMessage = apiStatus.getMessage();
    }
}

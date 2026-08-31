package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * Created by tuannt7 on 29/08/2023
 */
@Data
@AllArgsConstructor
public abstract class BaseException extends RuntimeException implements Serializable {
    private final HttpStatus status;
    private final ApiError apiError;

    protected BaseException(HttpStatus status, ApiStatus statusEnum) {
        this.status = status;
        this.apiError = new ApiError(statusEnum.name(), statusEnum.getMessage());
    }
}

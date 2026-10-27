package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import org.springframework.http.HttpStatus;

/**
 * Created by tuannt7 on 05/09/2023
 */
public class BadRequestException extends BaseException {
    public BadRequestException(ApiStatus statusEnum) {
        super(HttpStatus.BAD_REQUEST, statusEnum);
    }

    /** Keeps a specific errorCode for the client while adding detail such as the clashing value. */
    public BadRequestException(ApiStatus statusEnum, String message) {
        super(HttpStatus.BAD_REQUEST, new ApiError(statusEnum.name(), message));
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, new ApiError(ApiStatus.BAD_REQUEST.name(), message));
    }
}

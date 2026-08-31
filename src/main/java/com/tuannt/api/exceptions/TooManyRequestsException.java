package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import org.springframework.http.HttpStatus;

/**
 * Created by tuannt7 on 01/09/2026
 */
public class TooManyRequestsException extends BaseException {
    public TooManyRequestsException() {
        super(HttpStatus.TOO_MANY_REQUESTS, ApiStatus.TOO_MANY_REQUESTS);
    }
}

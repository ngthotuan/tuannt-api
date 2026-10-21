package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import org.springframework.http.HttpStatus;

/**
 * Created by tuannt7 on 21/10/2026
 */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(ApiStatus statusEnum) {
        super(HttpStatus.UNAUTHORIZED, statusEnum);
    }
}

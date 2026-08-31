package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import org.springframework.http.HttpStatus;

/**
 * Created by tuannt7 on 29/08/2023
 */
public class InternalException extends BaseException {
    public InternalException(ApiStatus statusEnum) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, statusEnum);
    }
}

package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import org.springframework.http.HttpStatus;

/**
 * Created by tuannt7 on 17/03/2023
 */
public class NotFoundException extends BaseException {
    public NotFoundException(ApiStatus statusEnum) {
        super(HttpStatus.NOT_FOUND, statusEnum);
    }
}

package com.tuannt.api.exceptions;

import com.tuannt.api.constants.ApiStatus;
import org.springframework.http.HttpStatus;

/**
 * Created by tuannt7 on 09/11/2026
 * <p>
 * An upstream service (e.g. GitHub) failed or was unreachable: 502, not our own 500.
 */
public class ExternalServiceException extends BaseException {
    public ExternalServiceException(ApiStatus statusEnum) {
        super(HttpStatus.BAD_GATEWAY, statusEnum);
    }
}

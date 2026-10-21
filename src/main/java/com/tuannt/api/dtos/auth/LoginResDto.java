package com.tuannt.api.dtos.auth;

import lombok.Builder;
import lombok.Getter;

/**
 * Created by tuannt7 on 21/10/2026
 */

// Deliberately not a BaseDto: toString() must never serialize the token into a log line.
@Getter
@Builder
public class LoginResDto {
    private String token;
    /** Epoch millis, same convention as every other timestamp this API returns. */
    private long expiresAt;
    private UserDto user;
}

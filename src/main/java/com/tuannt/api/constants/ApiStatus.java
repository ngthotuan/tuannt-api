package com.tuannt.api.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by tuannt7 on 24/08/2023
 */
@Getter
@AllArgsConstructor
public enum ApiStatus {
    SUCCESS("Success"),
    UNSUPPORTED("Unsupported error"),
    UNAUTHORIZED("Unauthorized request"),
    FORBIDDEN("You do not have permission to do this"),
    INVALID_CREDENTIALS("Invalid username or password"),
    USERNAME_TAKEN("Username already exists"),
    WORD_NOT_FOUND("Word not found"),
    BAD_REQUEST("Bad request"),
    METHOD_NOT_ALLOW("Method not allow"),
    TOO_MANY_REQUESTS("Too many requests, please try again later"),
    INTERNAL_SERVER_ERROR("Server error"),
    EXTERNAL_SERVER_ERROR("External service error"),
    INVALID_TTL("Invalid ttl"),
    INVALID_SIGNATURE("Invalid signature"),
    DUPLICATE_REFERENCE("Duplicate reference id"),
    TRANSACTION_NOT_FOUND("Transaction not found"),
    ;
    private final String message;
}


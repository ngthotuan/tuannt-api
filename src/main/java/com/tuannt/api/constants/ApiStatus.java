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
    POST_NOT_FOUND("Post not found"),
    PROJECT_NOT_FOUND("Project not found"),
    MESSAGE_NOT_FOUND("Message not found"),
    USER_NOT_FOUND("User not found"),
    SLUG_TAKEN("Slug already exists"),
    CANNOT_CHANGE_SELF("You cannot delete or demote your own account"),
    LAST_ADMIN("The last remaining admin cannot be deleted or demoted"),
    CONFLICT("The data was changed at the same time, please retry"),
    BAD_REQUEST("Bad request"),
    METHOD_NOT_ALLOW("Method not allow"),
    TOO_MANY_REQUESTS("Too many requests, please try again later"),
    INTERNAL_SERVER_ERROR("Server error"),
    EXTERNAL_SERVER_ERROR("External service error"),
    ;
    private final String message;
}


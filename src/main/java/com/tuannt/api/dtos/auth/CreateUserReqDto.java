package com.tuannt.api.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuannt.api.constants.UserRole;
import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Getter
@Setter
public class CreateUserReqDto extends BaseDto {
    @NotBlank(message = "username is required")
    @Pattern(regexp = "[A-Za-z0-9_.-]{3,50}",
            message = "username must be 3-50 letters, digits, dots, dashes or underscores")
    private String username;

    // WRITE_ONLY keeps the password out of toString(), which LoggingAspect writes to the log.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 100, message = "password must be 8-100 characters")
    private String password;

    private UserRole role = UserRole.USER;
}

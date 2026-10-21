package com.tuannt.api.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Getter
@Setter
public class LoginReqDto extends BaseDto {
    @NotBlank(message = "username is required")
    @Size(max = 50, message = "username must be at most 50 characters")
    private String username;

    // WRITE_ONLY keeps the password out of toString(), which LoggingAspect writes to the log.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "password is required")
    @Size(max = 100, message = "password must be at most 100 characters")
    private String password;
}

package com.tuannt.api.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by tuannt7 on 27/10/2026
 * <p>
 * An admin resetting someone's password; no current password needed.
 */

@Getter
@Setter
public class SetPasswordReqDto extends BaseDto {
    // WRITE_ONLY keeps the password out of toString(), which LoggingAspect writes to the log.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 100, message = "password must be 8-100 characters")
    private String password;
}

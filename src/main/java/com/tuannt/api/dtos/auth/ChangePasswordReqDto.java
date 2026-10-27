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
 * A logged-in user changing their own password.
 */

@Getter
@Setter
public class ChangePasswordReqDto extends BaseDto {
    // WRITE_ONLY keeps both passwords out of toString(), which LoggingAspect writes to the log.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "currentPassword is required")
    @Size(max = 100, message = "currentPassword must be at most 100 characters")
    private String currentPassword;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "newPassword is required")
    @Size(min = 8, max = 100, message = "newPassword must be 8-100 characters")
    private String newPassword;
}

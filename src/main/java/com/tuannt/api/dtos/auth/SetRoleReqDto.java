package com.tuannt.api.dtos.auth;

import com.tuannt.api.constants.UserRole;
import com.tuannt.api.dtos.BaseDto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by tuannt7 on 27/10/2026
 */

@Getter
@Setter
public class SetRoleReqDto extends BaseDto {
    @NotNull(message = "role must be USER or ADMIN")
    private UserRole role;
}

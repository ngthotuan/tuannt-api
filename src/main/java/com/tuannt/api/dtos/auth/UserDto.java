package com.tuannt.api.dtos.auth;

import com.tuannt.api.constants.UserRole;
import com.tuannt.api.dtos.BaseDto;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Getter
@Builder
public class UserDto extends BaseDto {
    private Long id;
    private String username;
    private UserRole role;
}

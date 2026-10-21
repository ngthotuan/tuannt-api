package com.tuannt.api.services;

import com.tuannt.api.dtos.auth.CreateUserReqDto;
import com.tuannt.api.dtos.auth.LoginReqDto;
import com.tuannt.api.dtos.auth.LoginResDto;
import com.tuannt.api.dtos.auth.UserDto;

/**
 * Created by tuannt7 on 21/10/2026
 */
public interface AuthService {
    LoginResDto login(LoginReqDto req, String ip);

    UserDto me(Long userId);

    UserDto createUser(CreateUserReqDto req);
}

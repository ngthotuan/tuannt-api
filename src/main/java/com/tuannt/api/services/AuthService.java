package com.tuannt.api.services;

import com.tuannt.api.constants.UserRole;
import com.tuannt.api.dtos.auth.ChangePasswordReqDto;
import com.tuannt.api.dtos.auth.CreateUserReqDto;
import com.tuannt.api.dtos.auth.LoginReqDto;
import com.tuannt.api.dtos.auth.LoginResDto;
import com.tuannt.api.dtos.auth.UserDto;

import java.util.List;

/**
 * Created by tuannt7 on 21/10/2026
 */
public interface AuthService {
    LoginResDto login(LoginReqDto req, String ip);

    UserDto me(Long userId);

    UserDto createUser(CreateUserReqDto req);

    /** Wrong current password is a 401 INVALID_CREDENTIALS. */
    void changeOwnPassword(Long userId, ChangePasswordReqDto req);

    List<UserDto> listUsers();

    /** Admin reset, no current password needed. */
    void setPassword(Long userId, String password);

    /** actorId is the admin making the change; nobody can demote themselves or the last admin. */
    UserDto setRole(Long actorId, Long userId, UserRole role);

    /** Same guards as setRole; the user's saved words go with the account. */
    void deleteUser(Long actorId, Long userId);
}

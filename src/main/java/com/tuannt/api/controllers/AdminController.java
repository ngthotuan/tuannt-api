package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.auth.CreateUserReqDto;
import com.tuannt.api.dtos.auth.UserDto;
import com.tuannt.api.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by tuannt7 on 21/10/2026
 * <p>
 * ADMIN only (enforced in SecurityConfig). There is no public sign-up.
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_ADMIN_PATH)
@RequiredArgsConstructor
public class AdminController {
    private final AuthService authService;

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserReqDto req) {
        return authService.createUser(req);
    }
}

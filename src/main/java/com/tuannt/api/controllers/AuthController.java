package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.auth.LoginReqDto;
import com.tuannt.api.dtos.auth.LoginResDto;
import com.tuannt.api.dtos.auth.UserDto;
import com.tuannt.api.services.AuthService;
import com.tuannt.api.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Slf4j
@Validated
@RestController
@RequestMapping(ApiPaths.API_AUTH_PATH)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final HttpServletRequest request;
    private final CommonUtil commonUtil;

    @PostMapping("/login")
    public LoginResDto login(@Valid @RequestBody LoginReqDto req) {
        return authService.login(req, commonUtil.clientIp(request));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(Long.valueOf(jwt.getSubject()));
    }

}

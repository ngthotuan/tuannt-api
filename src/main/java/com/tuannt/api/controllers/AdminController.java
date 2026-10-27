package com.tuannt.api.controllers;

import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.dtos.auth.CreateUserReqDto;
import com.tuannt.api.dtos.auth.SetPasswordReqDto;
import com.tuannt.api.dtos.auth.SetRoleReqDto;
import com.tuannt.api.dtos.auth.UserDto;
import com.tuannt.api.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/users")
    public List<UserDto> listUsers() {
        return authService.listUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserReqDto req) {
        return authService.createUser(req);
    }

    @PutMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@PathVariable Long id, @Valid @RequestBody SetPasswordReqDto req) {
        authService.setPassword(id, req.getPassword());
    }

    @PutMapping("/users/{id}/role")
    public UserDto setRole(@PathVariable Long id, @Valid @RequestBody SetRoleReqDto req,
                           @AuthenticationPrincipal Jwt jwt) {
        return authService.setRole(actorId(jwt), id, req.getRole());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        authService.deleteUser(actorId(jwt), id);
    }

    /** The token subject is the app_user id, set at login. */
    private Long actorId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

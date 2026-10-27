package com.tuannt.api.services.impl;

import com.tuannt.api.configs.AuthConfig;
import com.tuannt.api.configs.SecurityConfig;
import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.dtos.auth.CreateUserReqDto;
import com.tuannt.api.dtos.auth.LoginReqDto;
import com.tuannt.api.dtos.auth.LoginResDto;
import com.tuannt.api.dtos.auth.UserDto;
import com.tuannt.api.entities.AppUser;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.TooManyRequestsException;
import com.tuannt.api.exceptions.UnauthorizedException;
import com.tuannt.api.repositories.AppUserRepository;
import com.tuannt.api.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Created by tuannt7 on 21/10/2026
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final int MAX_LOGINS_PER_WINDOW = 10;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    /** BCrypt only ever reads the first 72 bytes, and Spring Security 6.4.5+ rejects longer input. */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthConfig authConfig;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional(readOnly = true)
    public LoginResDto login(LoginReqDto req, String ip) {
        if (!rateLimiter.tryAcquire("login:" + ip, MAX_LOGINS_PER_WINDOW, LOGIN_WINDOW)) {
            log.warn("Login rate limited for ip: {}", ip);
            throw new TooManyRequestsException();
        }
        // No stored password can be longer, so this is simply a wrong password, not a 500.
        if (tooLongForBcrypt(req.getPassword())) {
            throw new UnauthorizedException(ApiStatus.INVALID_CREDENTIALS);
        }
        // Same error for unknown user and wrong password, so the response doesn't reveal which
        // usernames exist.
        AppUser user = userRepository.findByUsername(req.getUsername().trim())
                .filter(u -> passwordEncoder.matches(req.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    log.info("Failed login for username: {} ip: {}", req.getUsername(), ip);
                    return new UnauthorizedException(ApiStatus.INVALID_CREDENTIALS);
                });

        Instant now = Instant.now();
        Instant expiresAt = now.plus(authConfig.getTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(SecurityConfig.USERNAME_CLAIM, user.getUsername())
                .claim(SecurityConfig.ROLE_CLAIM, user.getRole().name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        log.info("Login succeeded for user id: {} username: {}", user.getId(), user.getUsername());
        return LoginResDto.builder()
                .token(token)
                .expiresAt(expiresAt.toEpochMilli())
                .user(toDto(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        // A valid token can outlive its account; treat a deleted user as logged out.
        return userRepository.findById(userId)
                .map(this::toDto)
                .orElseThrow(() -> new UnauthorizedException(ApiStatus.UNAUTHORIZED));
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserReqDto req) {
        if (tooLongForBcrypt(req.getPassword())) {
            throw new BadRequestException("password must be at most " + BCRYPT_MAX_BYTES + " bytes");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException(ApiStatus.USERNAME_TAKEN);
        }
        AppUser user = new AppUser();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setCreatedAt(Instant.now());
        AppUser saved = userRepository.save(user);
        log.info("User created id: {} username: {} role: {}", saved.getId(), saved.getUsername(), saved.getRole());
        return toDto(saved);
    }

    private boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }

    private UserDto toDto(AppUser user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}

package com.tuannt.api.services.impl;

import com.tuannt.api.configs.AuthConfig;
import com.tuannt.api.configs.SecurityConfig;
import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.constants.UserRole;
import com.tuannt.api.dtos.auth.ChangePasswordReqDto;
import com.tuannt.api.dtos.auth.CreateUserReqDto;
import com.tuannt.api.dtos.auth.LoginReqDto;
import com.tuannt.api.dtos.auth.LoginResDto;
import com.tuannt.api.dtos.auth.UserDto;
import com.tuannt.api.entities.AppUser;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.NotFoundException;
import com.tuannt.api.exceptions.TooManyRequestsException;
import com.tuannt.api.exceptions.UnauthorizedException;
import com.tuannt.api.repositories.AppUserRepository;
import com.tuannt.api.repositories.VocabReviewLogRepository;
import com.tuannt.api.repositories.VocabReviewRepository;
import com.tuannt.api.repositories.VocabSavedWordRepository;
import com.tuannt.api.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
import java.util.List;

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
    private final VocabSavedWordRepository savedWordRepository;
    private final VocabReviewRepository reviewRepository;
    private final VocabReviewLogRepository reviewLogRepository;

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
        requireBcryptLength(req.getPassword(), "password");
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

    @Override
    @Transactional
    public void changeOwnPassword(Long userId, ChangePasswordReqDto req) {
        // Same budget as login: a stolen token must not become a way to brute-force the password.
        if (!rateLimiter.tryAcquire("password:" + userId, MAX_LOGINS_PER_WINDOW, LOGIN_WINDOW)) {
            log.warn("Password change rate limited for user id: {}", userId);
            throw new TooManyRequestsException();
        }
        requireBcryptLength(req.getNewPassword(), "newPassword");
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(ApiStatus.UNAUTHORIZED));
        if (tooLongForBcrypt(req.getCurrentPassword())
                || !passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            log.info("Password change with wrong current password for user id: {}", userId);
            throw new UnauthorizedException(ApiStatus.INVALID_CREDENTIALS);
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        log.info("Password changed by user id: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAll(Sort.by("id")).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void setPassword(Long userId, String password) {
        requireBcryptLength(password, "password");
        AppUser user = findUser(userId);
        user.setPasswordHash(passwordEncoder.encode(password));
        log.info("Password reset by admin for user id: {}", userId);
    }

    @Override
    @Transactional
    public UserDto setRole(Long actorId, Long userId, UserRole role) {
        AppUser user = findUser(userId);
        if (user.getRole() == role) {
            return toDto(user);
        }
        if (user.getRole() == UserRole.ADMIN) {
            guardAdminRemoval(actorId, user);
        }
        user.setRole(role);
        log.info("Role changed for user id: {} to {} by user id: {}", userId, role, actorId);
        return toDto(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long actorId, Long userId) {
        AppUser user = findUser(userId);
        if (actorId.equals(userId)) {
            throw new BadRequestException(ApiStatus.CANNOT_CHANGE_SELF);
        }
        if (user.getRole() == UserRole.ADMIN) {
            guardAdminRemoval(actorId, user);
        }
        // No foreign keys in the schema, so dependent rows are removed explicitly.
        savedWordRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        reviewLogRepository.deleteByUserId(userId);
        userRepository.delete(user);
        log.info("User deleted id: {} username: {} by user id: {}", userId, user.getUsername(), actorId);
    }

    /**
     * Rules for taking an admin away, by demotion or deletion. Existing tokens are not revoked:
     * a removed admin's token keeps its ADMIN claim until it expires.
     */
    private void guardAdminRemoval(Long actorId, AppUser admin) {
        if (actorId.equals(admin.getId())) {
            throw new BadRequestException(ApiStatus.CANNOT_CHANGE_SELF);
        }
        List<AppUser> admins = userRepository.lockByRole(UserRole.ADMIN);
        if (admins.stream().allMatch(a -> a.getId().equals(admin.getId()))) {
            throw new BadRequestException(ApiStatus.LAST_ADMIN);
        }
    }

    private AppUser findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException(ApiStatus.USER_NOT_FOUND));
    }

    /** Shared by every place that sets a password, so the byte limit cannot drift between them. */
    private void requireBcryptLength(String password, String field) {
        if (tooLongForBcrypt(password)) {
            throw new BadRequestException(field + " must be at most " + BCRYPT_MAX_BYTES + " bytes");
        }
    }

    private boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }

    private UserDto toDto(AppUser user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt() == null ? null : user.getCreatedAt().toEpochMilli())
                .build();
    }
}

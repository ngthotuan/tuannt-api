package com.tuannt.api.services.impl;

import com.tuannt.api.configs.AuthConfig;
import com.tuannt.api.constants.UserRole;
import com.tuannt.api.dtos.auth.ChangePasswordReqDto;
import com.tuannt.api.entities.AppUser;
import com.tuannt.api.exceptions.BadRequestException;
import com.tuannt.api.exceptions.BaseException;
import com.tuannt.api.exceptions.UnauthorizedException;
import com.tuannt.api.repositories.AppUserRepository;
import com.tuannt.api.repositories.VocabReviewLogRepository;
import com.tuannt.api.repositories.VocabReviewRepository;
import com.tuannt.api.repositories.VocabSavedWordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {
    // Low cost factor keeps the test fast; the production encoder is the default strength.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final VocabSavedWordRepository savedWords = mock(VocabSavedWordRepository.class);
    private final VocabReviewRepository reviews = mock(VocabReviewRepository.class);
    private final VocabReviewLogRepository reviewLogs = mock(VocabReviewLogRepository.class);
    private final AuthServiceImpl service = new AuthServiceImpl(users, encoder, mock(JwtEncoder.class),
            new AuthConfig(), new RateLimiter(), savedWords, reviews, reviewLogs);

    @Test
    void adminCannotDemoteOrDeleteThemselves() {
        AppUser me = user(1L, UserRole.ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(me));

        assertThat(errorCode(() -> service.setRole(1L, 1L, UserRole.USER))).isEqualTo("CANNOT_CHANGE_SELF");
        assertThat(errorCode(() -> service.deleteUser(1L, 1L))).isEqualTo("CANNOT_CHANGE_SELF");
        assertThat(me.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void lastAdminCannotBeDemotedOrDeleted() {
        // Actor 9 holds a token with the ADMIN claim but is no longer an admin in the database.
        AppUser onlyAdmin = user(1L, UserRole.ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(onlyAdmin));
        when(users.lockByRole(UserRole.ADMIN)).thenReturn(List.of(onlyAdmin));

        assertThat(errorCode(() -> service.setRole(9L, 1L, UserRole.USER))).isEqualTo("LAST_ADMIN");
        assertThat(errorCode(() -> service.deleteUser(9L, 1L))).isEqualTo("LAST_ADMIN");
        verify(users, never()).delete(onlyAdmin);
    }

    @Test
    void anotherAdminCanBeDemotedWhileOneRemains() {
        AppUser target = user(2L, UserRole.ADMIN);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.lockByRole(UserRole.ADMIN)).thenReturn(List.of(user(1L, UserRole.ADMIN), target));

        assertThat(service.setRole(1L, 2L, UserRole.USER).getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void deletingAUserAlsoDropsTheirSavedWordsAndReviews() {
        AppUser target = user(5L, UserRole.USER);
        when(users.findById(5L)).thenReturn(Optional.of(target));

        service.deleteUser(1L, 5L);

        verify(savedWords).deleteByUserId(5L);
        verify(reviews).deleteByUserId(5L);
        verify(reviewLogs).deleteByUserId(5L);
        verify(users).delete(target);
    }

    @Test
    void changingOwnPasswordChecksTheCurrentOne() {
        AppUser me = user(3L, UserRole.USER);
        me.setPasswordHash(encoder.encode("old-password"));
        when(users.findById(3L)).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> service.changeOwnPassword(3L, change("wrong-password", "new-password")))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((UnauthorizedException) e).getApiError().getErrorCode())
                .isEqualTo("INVALID_CREDENTIALS");

        service.changeOwnPassword(3L, change("old-password", "new-password"));
        assertThat(encoder.matches("new-password", me.getPasswordHash())).isTrue();
    }

    @Test
    void passwordsOverTheBcryptLimitAreRejected() {
        // 30 three-byte characters: within the 100-character limit, over the 72-byte one.
        String tooLong = "ế".repeat(30);
        assertThat(errorCode(() -> service.setPassword(3L, tooLong))).isEqualTo("BAD_REQUEST");
    }

    private static String errorCode(Runnable call) {
        try {
            call.run();
        } catch (BaseException e) {
            return e.getApiError().getErrorCode();
        }
        throw new AssertionError("expected a BaseException");
    }

    private static ChangePasswordReqDto change(String current, String next) {
        ChangePasswordReqDto req = new ChangePasswordReqDto();
        req.setCurrentPassword(current);
        req.setNewPassword(next);
        return req;
    }

    private static AppUser user(Long id, UserRole role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setPasswordHash("unused");
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
